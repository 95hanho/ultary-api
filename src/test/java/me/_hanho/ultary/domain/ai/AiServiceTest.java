package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import me._hanho.ultary.domain.ai.model.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {
    @Mock GeminiClient client;
    @Mock AiLogService logs;
    AiService service;
    @BeforeEach void init() { service = new AiService(client, new ObjectMapper(), logs); }
    ModerationJob job() {
        ModerationJob j = new ModerationJob(); j.setJobId(7L); j.setUserNo(101L); j.setFeedId(120L);
        j.setTargetType("COMMENT"); j.setTargetId(815L); j.setContentVersion(2L); j.setAttemptCount(1); j.setContent("hello"); return j;
    }
    @Test void unsafeIsAVerdictNotAnInfrastructureFailure() {
        when(client.generateJson(anyString(),anyString())).thenReturn("{\"safe\":false,\"category\":\"ABUSE\"}");
        var result = service.moderate(job());
        assertThat(result.safe()).isFalse(); assertThat(result.failed()).isFalse();
        var c = ArgumentCaptor.forClass(AiRequestLog.class); verify(logs).write(c.capture());
        assertThat(c.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(c.getValue().getModerationJobId()).isEqualTo(7L);
        assertThat(c.getValue().getTargetId()).isEqualTo(815L);
        assertThat(c.getValue().getContentVersion()).isEqualTo(2L);
    }
    @Test void malformedVerdictDoesNotApproveOrRejectContent() {
        when(client.generateJson(anyString(),anyString())).thenReturn("{\"category\":\"ABUSE\"}");
        var result = service.moderate(job());
        assertThat(result.failed()).isTrue(); assertThat(result.safe()).isNull(); assertThat(result.retryable()).isTrue();
    }
    @Test void timeoutIsRecordedWithTargetAttemptAndNoUserText() {
        when(client.generateJson(anyString(),anyString())).thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT",null,"Response deadline exceeded",true));
        var result = service.moderate(job());
        assertThat(result.failed()).isTrue(); assertThat(result.errorCode()).isEqualTo("GEMINI_TIMEOUT");
        var c=ArgumentCaptor.forClass(AiRequestLog.class);verify(logs).write(c.capture());
        assertThat(c.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(c.getValue().getErrorMessage()).isEqualTo("Response deadline exceeded");
        assertThat(c.getValue().getPrompt()).doesNotContain("hello");
        assertThat(c.getValue().getInspectionContent()).isEqualTo("hello");
    }
    @Test void emptyCaptionRequiresNoExternalCall() {
        var j=job();j.setContent(null);assertThat(service.moderate(j).safe()).isTrue();verify(client,never()).generateJson(anyString(),anyString());
    }
    @Test void tagRecommendationStillUsesSynchronousFallbackAndSanitizesTags() {
        when(client.isEnabled()).thenReturn(true);
        when(client.generateJson(anyString(),anyString())).thenReturn("{\"hashtags\":[\"dog\",\"#walk\",\"dog\",\"invalid!!\"]}");
        assertThat(service.recommendHashtags(101L,"walk")).containsExactly("dog","walk");
        when(client.generateJson(anyString(),anyString())).thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT"));
        assertThatThrownBy(() -> service.recommendHashtags(101L,"walk"))
            .isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class).hasMessageContaining("잠시 사용할 수 없습니다");
        verify(client,times(4)).generateJson(anyString(),anyString());
    }
    @Test void recommendationCanRecoverOnTheThirdAttempt() {
        when(client.isEnabled()).thenReturn(true);
        when(client.generateJson(anyString(),anyString())).thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT"))
            .thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT")).thenReturn("{\"hashtags\":[\"dog\"]}");
        assertThat(service.recommendHashtags(101L,"walk")).containsExactly("dog");
        verify(client,times(3)).generateJson(anyString(),anyString());
    }

    @Test void feedCreationRejectsUnsafeTextWithAReasonAndKeepsTheInspectionLog() {
        when(client.generateJson(anyString(),anyString())).thenReturn("{\"safe\":false,\"category\":\"ABUSE\"}");
        assertThatThrownBy(() -> service.assertFeedCreationAllowed(101L,"unsafe"))
                .isInstanceOfSatisfying(me._hanho.ultary.common.exception.BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(me._hanho.ultary.common.exception.ErrorCode.AI_CONTENT_BLOCKED);
                    assertThat(ex.getMessage()).contains("욕설·비방");
                });
        var row=ArgumentCaptor.forClass(AiRequestLog.class);verify(logs).write(row.capture());
        assertThat(row.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(row.getValue().getModerationJobId()).isNull();
        assertThat(row.getValue().getTargetType()).isEqualTo("FEED");
    }

    @Test void feedCreationTimeoutIsAServiceFailureAndNeverAUserViolation() {
        when(client.generateJson(anyString(),anyString())).thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT"));
        assertThatThrownBy(() -> service.assertFeedCreationAllowed(101L,"content"))
                .isInstanceOfSatisfying(me._hanho.ultary.common.exception.BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(me._hanho.ultary.common.exception.ErrorCode.AI_CHECK_UNAVAILABLE);
                    assertThat(ex.getMessage()).contains("응답 시간");
                });
        var row=ArgumentCaptor.forClass(AiRequestLog.class);verify(logs).write(row.capture());
        assertThat(row.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test void updateFailureLogRetainsTheEditedTextAndFeedIdentity() {
        when(client.generateJson(anyString(),anyString())).thenThrow(new GeminiClient.GeminiException("GEMINI_TIMEOUT"));
        assertThatThrownBy(() -> service.assertFeedUpdateAllowed(101L,120L,"edited draft"))
            .isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
        var row=ArgumentCaptor.forClass(AiRequestLog.class);verify(logs).write(row.capture());
        assertThat(row.getValue().getInspectionContent()).isEqualTo("edited draft");
        assertThat(row.getValue().getTargetId()).isEqualTo(120L);
        assertThat(row.getValue().getModerationJobId()).isNull();
    }
    @Test void feedCreationAllowsApprovedContent() {
        when(client.generateJson(anyString(),anyString())).thenReturn("{\"safe\":true}");
        assertThatCode(() -> service.assertFeedCreationAllowed(101L,"content")).doesNotThrowAnyException();
    }
}
