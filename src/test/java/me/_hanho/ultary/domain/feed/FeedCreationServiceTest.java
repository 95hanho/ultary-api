package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

class FeedCreationServiceTest {
    AiService ai = mock(AiService.class);
    FeedService feeds = mock(FeedService.class);
    FeedCreationService service = new FeedCreationService(ai, feeds);
    UserPrincipal user = new UserPrincipal(101L,"test");
    CreateFeedRequest request;

    @BeforeEach void fixture() {
        request = new CreateFeedRequest(); request.setContent("draft");
        request.setMedia(List.of(new CreateFeedRequest.MediaItem()));
    }

    @Test void savesOnlyAfterTheSynchronousCheckReturns() {
        FeedResponse saved = mock(FeedResponse.class);
        when(feeds.create(user,request)).thenReturn(saved);
        assertThat(service.create(user,request)).isSameAs(saved);
        var order = inOrder(ai,feeds);
        order.verify(ai).assertFeedCreationAllowed(101L,"draft");
        order.verify(feeds).create(user,request);
    }

    @Test void neitherUnsafeVerdictsNorServiceFailuresSaveAFeed() {
        for (ErrorCode error : new ErrorCode[]{ErrorCode.AI_CONTENT_BLOCKED,ErrorCode.AI_CHECK_UNAVAILABLE}) {
            doThrow(new BusinessException(error)).when(ai).assertFeedCreationAllowed(101L,"draft");
            assertThatThrownBy(() -> service.create(user,request)).isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(feeds);
    }

    @Test void missingMediaDoesNotInvokeTheProvider() {
        request.setMedia(List.of());
        assertThatThrownBy(() -> service.create(user,request)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FEED_MEDIA_REQUIRED));
        verifyNoInteractions(ai,feeds);
    }
}
