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

    @Test void checksBeforeSaving() {
        FeedResponse saved = mock(FeedResponse.class);
        when(feeds.create(user,request)).thenReturn(saved);
        assertThat(service.create(user,request)).isSameAs(saved);
        verify(ai).assertFeedCreationAllowed(user.getUserNo(), "draft");
        var order = inOrder(ai, feeds);
        order.verify(ai).assertFeedCreationAllowed(user.getUserNo(), "draft");
        order.verify(feeds).create(user,request);
    }

    @Test void persistenceErrorsAreStillReported() {
        doThrow(new BusinessException(ErrorCode.FEED_MEDIA_REQUIRED)).when(feeds).create(user, request);
        assertThatThrownBy(() -> service.create(user,request)).isInstanceOf(BusinessException.class);
        verify(ai).assertFeedCreationAllowed(user.getUserNo(), "draft");
    }

    @Test void unsafeContentIsNeverSaved() {
        doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED)).when(ai).assertFeedCreationAllowed(user.getUserNo(), "draft");
        assertThatThrownBy(() -> service.create(user,request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(feeds);
    }
    @Test void updatesInspectBeforeSavingAndUnsafeContentPreservesOriginal() {
        var update = new me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest(); update.setContent("changed");
        when(feeds.getDetail(user, 17L)).thenReturn(FeedResponse.builder().userNo(user.getUserNo()).content("original").build());
        var controller = new FeedController(feeds, service);
        controller.update(user,17L,update);
        var order = inOrder(ai,feeds);
        order.verify(ai).assertFeedUpdateAllowed(user.getUserNo(),17L,"changed");
        order.verify(feeds).update(user,17L,update);
        clearInvocations(feeds);
        doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED)).when(ai).assertFeedUpdateAllowed(user.getUserNo(),17L,"changed");
        assertThatThrownBy(() -> service.update(user,17L,update)).isInstanceOf(BusinessException.class);
        verify(feeds,never()).update(any(),any(),any());
    }
    @Test void missingMediaDoesNotInvokeTheProvider() {
        request.setMedia(List.of());
        assertThatThrownBy(() -> service.create(user,request)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FEED_MEDIA_REQUIRED));
        verifyNoInteractions(ai,feeds);
    }
}
