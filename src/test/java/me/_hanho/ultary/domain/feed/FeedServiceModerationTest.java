package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.ai.ContentModerationKind;
import me._hanho.ultary.domain.feed.dto.request.CreateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.report.ReportService;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.tag.TagMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class FeedServiceModerationTest {

	@Mock
	private FeedMapper feedMapper;
	@Mock
	private FileService fileService;
	@Mock
	private PetMapper petMapper;
	@Mock
	private PetService petService;
	@Mock
	private TagMapper tagMapper;
	@Mock
	private UserMapper userMapper;
	@Mock
	private NeighborService neighborService;
	@Mock
	private NotificationService notificationService;
	@Mock
	private PrivacyService privacyService;
	@Mock
	private AiService aiService;
	@Mock
	private ReportService reportService;

	@InjectMocks
	private FeedService feedService;

	@Test
	void createComment_blocked_doesNotInsert() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);

		Feed feed = new Feed();
		feed.setFeedId(1L);
		feed.setUserNo(102L);
		feed.setVisibility("PUBLIC");
		when(feedMapper.findActiveByFeedId(1L)).thenReturn(feed);

		me._hanho.ultary.domain.user.model.User author = new me._hanho.ultary.domain.user.model.User();
		author.setUserNo(102L);
		author.setWithdrawalStatus("ACTIVE");
		when(userMapper.findByUserNo(102L)).thenReturn(author);
		when(privacyService.feedAudience(102L, "PUBLIC")).thenReturn("PUBLIC");

		doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED))
				.when(aiService).assertContentSafe(
						eq(101L), eq(1L), anyString(), eq(ContentModerationKind.COMMENT));

		CreateCommentRequest request = new CreateCommentRequest();
		request.setContent("나쁜말");

		assertThatThrownBy(() -> feedService.createComment(principal, 1L, request))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));

		verify(feedMapper, never()).insertComment(any());
	}

	@Test
	void createFeed_blocked_doesNotInsert() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);

		doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED))
				.when(aiService).assertContentSafe(
						eq(101L), isNull(), anyString(), eq(ContentModerationKind.FEED));

		CreateFeedRequest request = new CreateFeedRequest();
		request.setContent("유해 게시글");
		CreateFeedRequest.MediaItem media = new CreateFeedRequest.MediaItem();
		media.setFileId(1L);
		request.setMedia(List.of(media));

		assertThatThrownBy(() -> feedService.create(principal, request))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));

		verify(feedMapper, never()).insertFeed(any());
	}

	@Test
	void updateFeed_blocked_doesNotUpdate() {
		UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
		when(principal.getUserNo()).thenReturn(101L);

		Feed feed = new Feed();
		feed.setFeedId(1L);
		feed.setUserNo(101L);
		when(feedMapper.findActiveByFeedId(1L)).thenReturn(feed);

		doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED))
				.when(aiService).assertContentSafe(
						eq(101L), eq(1L), anyString(), eq(ContentModerationKind.FEED));

		UpdateFeedRequest request = new UpdateFeedRequest();
		request.setContent("유해 수정");

		assertThatThrownBy(() -> feedService.update(principal, 1L, request))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));

		verify(feedMapper, never()).updateFeed(any());
	}
}
