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
import me._hanho.ultary.common.validation.HashtagRules;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.GeminiClient.GeminiException;
import me._hanho.ultary.domain.ai.dto.GeminiHashtagResult;
import me._hanho.ultary.domain.ai.model.AiRequestLog;
import me._hanho.ultary.domain.ai.model.ModerationJob;

/**
 * Gemini 비즈니스 호출. 원문·API Key는 DB에 넣지 않는다.
 * 추천은 총 3회 동기 시도, 게시글·댓글·답글은 DB 작업자로 검사한다.
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
	private final AiLogService aiLogService;

	/**
	 * 태그 추천은 총 3회 시도하고 최종 실패는 서비스 오류로 전달한다.
	 * 정상 응답이 빈 목록일 때만 호출자가 DB 추천으로 보완한다.
	 */
	public List<String> recommendHashtags(Long userNo, String content) {
		if (!StringUtils.hasText(content)) {
			return List.of();
		}
		if (!geminiClient.isEnabled()) throw new BusinessException(ErrorCode.AI_CHECK_UNAVAILABLE, "서버 오류로 태그 추천을 잠시 사용할 수 없습니다. 불편을 드려 죄송합니다. 잠시 후 다시 시도해 주세요.");
		for (int attempt = 1; attempt <= 3; attempt++) {
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
			if (attempt == 3) throw new BusinessException(ErrorCode.AI_CHECK_UNAVAILABLE, "서버 오류로 태그 추천을 잠시 사용할 수 없습니다. 불편을 드려 죄송합니다. 잠시 후 다시 시도해 주세요.");
		}
		}
		throw new IllegalStateException("Recommendation attempts exhausted");
	}

	/** New feed creation waits for a verdict; provider failure is a service error. */
	public void assertFeedCreationAllowed(Long userNo, String content) {
        assertFeedAllowed(userNo, null, content, "CREATE");
    }
    public void assertFeedUpdateAllowed(Long userNo, Long feedId, String content) {
        assertFeedAllowed(userNo, feedId, content, "UPDATE");
    }
    private void assertFeedAllowed(Long userNo, Long feedId, String content, String operation) {
		ModerationJob inspection = new ModerationJob();
		inspection.setUserNo(userNo); inspection.setTargetType("FEED");
		inspection.setFeedId(feedId); inspection.setTargetId(feedId);
        inspection.setOperation(operation); inspection.setAttemptCount(1); inspection.setContent(content);
		ModerationResult verdict;
		try {
			verdict = moderate(inspection);
		} catch (Exception ex) {
			log.error("[AI feed creation] inspection/logging unavailable cause={}", ex.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.AI_CHECK_UNAVAILABLE);
		}
		if (verdict.failed()) {
			String reason = switch (verdict.errorCode()) {
				case "GEMINI_TIMEOUT" -> "AI 검사 응답 시간이 초과되어 게시글이 저장되지 않았습니다. 잠시 후 다시 시도해 주세요.";
				case "GEMINI_HTTP_429" -> "AI 검사 요청이 많아 게시글이 저장되지 않았습니다. 잠시 후 다시 시도해 주세요.";
				default -> ErrorCode.AI_CHECK_UNAVAILABLE.getMessage();
			};
			throw new BusinessException(ErrorCode.AI_CHECK_UNAVAILABLE, reason);
		}
		if (!Boolean.TRUE.equals(verdict.safe())) {
			String reason = switch (verdict.category() == null ? "OTHER" : verdict.category()) {
				case "SPAM" -> "스팸·광고성 내용";
				case "ABUSE" -> "욕설·비방 표현";
				case "HARASSMENT" -> "괴롭힘 표현";
				case "SEXUAL" -> "음란한 표현";
				case "VIOLENCE" -> "폭력적인 표현";
				case "HATE" -> "혐오 표현";
				case "PRIVACY" -> "개인정보 침해 내용";
				default -> "부적절한 표현";
			};
			throw new BusinessException(ErrorCode.AI_CONTENT_BLOCKED,
					reason + "이 포함되어 게시글을 작성·수정할 수 없습니다. 내용을 수정해 다시 시도해 주세요.");
		}
	}

	/** Shared inspection/logging for synchronous creation and the durable worker. No content transaction. */
	public ModerationResult moderate(ModerationJob job) {
		long started = System.nanoTime();
		ModerationResult result;
		Integer httpStatus = null;
		try {
			if (!StringUtils.hasText(job.getContent())) {
				result = ModerationResult.allowed();
			} else {
				String json = geminiClient.generateJson(AiPrompts.contentModerationSystem(),
						AiPrompts.contentModerationUser(job.getContent()));
				var parsed = objectMapper.readTree(json);
				if (parsed == null || !parsed.path("safe").isBoolean()) {
					throw new GeminiException("GEMINI_INVALID_VERDICT", null, "Missing boolean safe field", true);
				}
				result = new ModerationResult(parsed.get("safe").booleanValue(),
						normalizeCategory(parsed.path("category").asText(null)), null, null, false);
			}
		} catch (Exception ex) {
			String code = errorCode(ex);
			String diagnostic = ex instanceof GeminiException gemini ? gemini.diagnostic() : ex.getClass().getSimpleName();
			boolean retryable = !(ex instanceof GeminiException gemini) || gemini.retryable();
			if (ex instanceof GeminiException gemini) httpStatus = gemini.httpStatus();
			result = new ModerationResult(null, null, code, diagnostic, retryable);
			log.warn("[AI moderation] jobId={} target={}:{} version={} attempt={} code={} cause={}",
					job.getJobId(), job.getTargetType(), job.getTargetId(), job.getContentVersion(), job.getAttemptCount(), code, diagnostic);
		}
		AiRequestLog row = new AiRequestLog();
		row.setUserNo(job.getUserNo()); row.setTargetFeedId(job.getFeedId());
        row.setInspectionContent(job.getContent());
		row.setFeatureType("FEED".equals(job.getTargetType()) ? AiFeatureTypes.FEED_CAPTION : AiFeatureTypes.COMMENT_FILTER);
		row.setModerationJobId(job.getJobId()); row.setTargetType(job.getTargetType()); row.setTargetId(job.getTargetId());
		row.setContentVersion(job.getContentVersion()); row.setAttemptNo(job.getAttemptCount());
		row.setLatencyMs((System.nanoTime() - started) / 1_000_000); row.setHttpStatus(httpStatus);
		row.setModelName(geminiClient.modelName()); row.setPrompt(summaryPrompt(job.getTargetType(), job.getContent()));
		row.setStatus(result.failed() ? "FAILED" : "SUCCESS"); row.setErrorCode(result.errorCode()); row.setErrorMessage(result.error());
		row.setResult(result.failed() ? null : "safe=" + result.safe() + "; category=" + result.category());
		aiLogService.write(row);
		return result;
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
			row.setErrorCode(errorCode);
			aiLogService.write(row);
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
		return "GEMINI_INVALID_JSON";
	}
}
