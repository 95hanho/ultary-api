package me._hanho.ultary.domain.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.validation.HashtagRules;
import me._hanho.ultary.domain.ai.GeminiClient.GeminiException;
import me._hanho.ultary.domain.ai.dto.GeminiHashtagResult;
import me._hanho.ultary.domain.ai.dto.GeminiModerationResult;
import me._hanho.ultary.domain.ai.model.AiRequestLog;

/**
 * Gemini 비즈니스 호출. 원문·API Key는 DB에 넣지 않는다.
 * 장애 시 호출자에게 예외를 넘기지 않고(모더레이션 차단 제외) empty/allow 로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

	private static final int MAX_HASHTAGS = 5;
	private static final int PROMPT_SUMMARY_MAX = 200;
	private static final int RESULT_SUMMARY_MAX = 500;
	private static final Pattern HASHTAG_PATTERN = Pattern.compile(HashtagRules.REGEX);
	private static final Set<String> MODERATION_CATEGORIES = Set.of(
			"SPAM", "ABUSE", "HARASSMENT", "SEXUAL", "VIOLENCE", "HATE", "PRIVACY", "OTHER");

	private final GeminiClient geminiClient;
	private final ObjectMapper objectMapper;
	private final AiRequestLogMapper aiRequestLogMapper;

	/**
	 * 게시글 내용 기반 해시태그 추천. 실패·비활성·빈 입력이면 empty.
	 * 호출자가 DB fallback 한다.
	 */
	public List<String> recommendHashtags(Long userNo, String content) {
		if (!StringUtils.hasText(content) || !geminiClient.isEnabled()) {
			return List.of();
		}
		long started = System.currentTimeMillis();
		try {
			String json = geminiClient.generateJson(
					AiPrompts.hashtagRecommendSystem(),
					AiPrompts.hashtagRecommendUser(content.trim()));
			GeminiHashtagResult parsed = objectMapper.readValue(json, GeminiHashtagResult.class);
			List<String> cleaned = sanitizeHashtags(parsed != null ? parsed.getHashtags() : null);
			saveLog(userNo, AiFeatureTypes.HASHTAG, null,
					summaryPrompt("HASHTAG", content),
					truncate("latencyMs=" + (System.currentTimeMillis() - started) + "; tags=" + cleaned, RESULT_SUMMARY_MAX),
					"SUCCESS", null, null);
			return cleaned;
		} catch (Exception ex) {
			String code = errorCode(ex);
			log.warn("[AiService] hashtag recommend failed code={} latencyMs={}",
					code, System.currentTimeMillis() - started);
			saveLog(userNo, AiFeatureTypes.HASHTAG, null,
					summaryPrompt("HASHTAG", content),
					null,
					"FAILED", code,
					truncate("latencyMs=" + (System.currentTimeMillis() - started), RESULT_SUMMARY_MAX));
			return List.of();
		}
	}

	/**
	 * 게시글·댓글·답글 저장 직전 검사.
	 * unsafe → {@link ErrorCode#AI_CONTENT_BLOCKED}.
	 * Gemini 장애 → 통과(등록 허용).
	 */
	public void assertContentSafe(
			Long userNo, Long feedId, String content, ContentModerationKind kind) {
		if (kind == null) {
			kind = ContentModerationKind.COMMENT;
		}
		if (!StringUtils.hasText(content) || !geminiClient.isEnabled()) {
			return;
		}
		long started = System.currentTimeMillis();
		String feature = kind == ContentModerationKind.FEED
				? AiFeatureTypes.FEED_CAPTION
				: AiFeatureTypes.COMMENT_FILTER;
		String label = switch (kind) {
			case FEED -> "FEED_MODERATION";
			case COMMENT -> "COMMENT_MODERATION";
			case REPLY -> "REPLY_MODERATION";
		};
		try {
			String json = geminiClient.generateJson(
					AiPrompts.contentModerationSystem(),
					AiPrompts.contentModerationUser(content.trim()));
			GeminiModerationResult parsed = objectMapper.readValue(json, GeminiModerationResult.class);
			boolean safe = parsed == null || parsed.isSafe();
			String category = normalizeCategory(parsed != null ? parsed.getCategory() : null);
			saveLog(userNo, feature, feedId,
					summaryPrompt(label, content),
					truncate("latencyMs=" + (System.currentTimeMillis() - started)
							+ "; safe=" + safe
							+ (category != null ? "; category=" + category : ""), RESULT_SUMMARY_MAX),
					"SUCCESS", null, null);
			if (!safe) {
				throw new BusinessException(ErrorCode.AI_CONTENT_BLOCKED);
			}
		} catch (BusinessException ex) {
			throw ex;
		} catch (Exception ex) {
			String code = errorCode(ex);
			log.warn("[AiService] moderation failed (allow) code={} latencyMs={} kind={}",
					code, System.currentTimeMillis() - started, kind);
			saveLog(userNo, feature, feedId,
					summaryPrompt(label, content),
					null,
					"FAILED", code,
					truncate("latencyMs=" + (System.currentTimeMillis() - started), RESULT_SUMMARY_MAX));
		}
	}

	List<String> sanitizeHashtags(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		for (String item : raw) {
			String normalized = HashtagRules.normalize(item);
			if (!StringUtils.hasText(normalized)) {
				continue;
			}
			if (!HASHTAG_PATTERN.matcher(normalized).matches()) {
				continue;
			}
			unique.add(normalized);
			if (unique.size() >= MAX_HASHTAGS) {
				break;
			}
		}
		return new ArrayList<>(unique);
	}

	private String normalizeCategory(String category) {
		if (!StringUtils.hasText(category)) {
			return null;
		}
		String upper = category.trim().toUpperCase(Locale.ROOT);
		return MODERATION_CATEGORIES.contains(upper) ? upper : "OTHER";
	}

	private void saveLog(
			Long userNo,
			String featureType,
			Long targetFeedId,
			String prompt,
			String result,
			String status,
			String errorCode,
			String errorMessage) {
		if (userNo == null) {
			return;
		}
		try {
			AiRequestLog row = new AiRequestLog();
			row.setUserNo(userNo);
			row.setFeatureType(featureType);
			row.setTargetFeedId(targetFeedId);
			row.setPrompt(prompt);
			row.setResult(result);
			row.setModelName(geminiClient.modelName());
			row.setStatus(status);
			if (StringUtils.hasText(errorCode) || StringUtils.hasText(errorMessage)) {
				String combined = (errorCode != null ? errorCode : "")
						+ (StringUtils.hasText(errorMessage) ? (" " + errorMessage) : "");
				row.setErrorMessage(truncate(combined.trim(), 1000));
			}
			aiRequestLogMapper.insert(row);
		} catch (Exception ex) {
			log.warn("[AiService] failed to write ai_request_log: {}", ex.toString());
		}
	}

	private static String summaryPrompt(String label, String content) {
		int len = content == null ? 0 : content.trim().length();
		return truncate(label + " contentLen=" + len, PROMPT_SUMMARY_MAX);
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max);
	}

	private static String errorCode(Exception ex) {
		if (ex instanceof GeminiException gemini) {
			return gemini.code();
		}
		String message = ex.getMessage();
		if (StringUtils.hasText(message)) {
			return message.length() > 80 ? message.substring(0, 80) : message;
		}
		return ex.getClass().getSimpleName();
	}
}
