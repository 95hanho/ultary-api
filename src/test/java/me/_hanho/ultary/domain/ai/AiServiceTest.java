package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.GeminiClient.GeminiException;
import me._hanho.ultary.domain.ai.model.AiRequestLog;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

	@Mock
	private GeminiClient geminiClient;

	@Mock
	private AiRequestLogMapper aiRequestLogMapper;

	private AiService aiService;

	@BeforeEach
	void setUp() {
		aiService = new AiService(geminiClient, new ObjectMapper(), aiRequestLogMapper);
	}

	@Test
	void recommendHashtags_returnsUpToFiveCleanTags() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString())).thenReturn(
				"{\"hashtags\":[\"강아지산책\",\"#한강산책\",\"강아지산책\",\"invalid!!\",\"반려견일상\",\"댕댕이\",\"초과태그\"]}");

		List<String> tags = aiService.recommendHashtags(101L, "오늘 몽이랑 한강 산책");

		assertThat(tags).containsExactly("강아지산책", "한강산책", "반려견일상", "댕댕이", "초과태그");
		verify(aiRequestLogMapper).insert(any(AiRequestLog.class));
	}

	@Test
	void recommendHashtags_geminiFailure_returnsEmptyForFallback() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString()))
				.thenThrow(new GeminiException("GEMINI_HTTP_FAILED"));

		assertThat(aiService.recommendHashtags(101L, "산책")).isEmpty();

		ArgumentCaptor<AiRequestLog> captor = ArgumentCaptor.forClass(AiRequestLog.class);
		verify(aiRequestLogMapper).insert(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
		assertThat(captor.getValue().getFeatureType()).isEqualTo(AiFeatureTypes.HASHTAG);
	}

	@Test
	void assertContentSafe_unsafe_throwsBlocked() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString())).thenReturn(
				"{\"safe\":false,\"category\":\"ABUSE\",\"reason\":\"욕설\"}");

		assertThatThrownBy(() -> aiService.assertContentSafe(
						101L, 1L, "욕설내용", ContentModerationKind.COMMENT))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));
	}

	@Test
	void assertContentSafe_feedUnsafe_throwsBlocked() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString())).thenReturn(
				"{\"safe\":false,\"category\":\"ABUSE\",\"reason\":\"욕설\"}");

		assertThatThrownBy(() -> aiService.assertContentSafe(
						101L, null, "유해 게시글", ContentModerationKind.FEED))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));

		ArgumentCaptor<AiRequestLog> captor = ArgumentCaptor.forClass(AiRequestLog.class);
		verify(aiRequestLogMapper).insert(captor.capture());
		assertThat(captor.getValue().getFeatureType()).isEqualTo(AiFeatureTypes.FEED_CAPTION);
	}

	@Test
	void assertContentSafe_safe_allows() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString())).thenReturn(
				"{\"safe\":true,\"category\":null,\"reason\":null}");

		aiService.assertContentSafe(101L, 1L, "귀여운 강아지네요", ContentModerationKind.COMMENT);
		verify(aiRequestLogMapper).insert(any(AiRequestLog.class));
	}

	@Test
	void assertContentSafe_geminiFailure_allowsWrite() {
		when(geminiClient.isEnabled()).thenReturn(true);
		when(geminiClient.modelName()).thenReturn("gemini-2.0-flash");
		when(geminiClient.generateJson(anyString(), anyString()))
				.thenThrow(new GeminiException("GEMINI_HTTP_FAILED"));

		aiService.assertContentSafe(101L, 1L, "아무말", ContentModerationKind.REPLY);
		ArgumentCaptor<AiRequestLog> captor = ArgumentCaptor.forClass(AiRequestLog.class);
		verify(aiRequestLogMapper).insert(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
	}

	@Test
	void recommendHashtags_disabled_skipsGemini() {
		when(geminiClient.isEnabled()).thenReturn(false);

		assertThat(aiService.recommendHashtags(101L, "산책")).isEmpty();
		verify(geminiClient, never()).generateJson(anyString(), anyString());
	}
}
