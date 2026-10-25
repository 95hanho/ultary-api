package me._hanho.ultary.domain.notification;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationItemResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationListResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationUnreadCountResponse;
import me._hanho.ultary.domain.notification.model.NeighborNotice;
import me._hanho.ultary.domain.notification.model.NotificationDraft;
import me._hanho.ultary.domain.notification.model.NotificationListRow;
import me._hanho.ultary.domain.notification.model.NotificationUpsert;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.ws.RealtimePush;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class NotificationService {

	static final String NEIGHBOR_REQUEST = "NEIGHBOR_REQUEST";
	static final String FEED_LIKE = "FEED_LIKE";
	static final String COMMENT_LIKE = "COMMENT_LIKE";
	static final String REPLY_LIKE = "REPLY_LIKE";
	static final String FEED_COMMENT = "FEED_COMMENT";
	static final String COMMENT_MENTION = "COMMENT_MENTION";
	static final String REPLY_MENTION = "REPLY_MENTION";
	static final String FEED_TAG = "FEED_TAG";
	static final String STORY_TAG = "STORY_TAG";
	static final String STORY_LIKE = "STORY_LIKE";

	private static final int DEFAULT_LIMIT = 30;
	private static final int MAX_LIMIT = 50;
	private static final int SNIPPET_LENGTH = 40;

	private static final List<String> COMMENT_DIRECT_TYPES = List.of(COMMENT_LIKE, COMMENT_MENTION);
	private static final List<String> REPLY_DIRECT_TYPES = List.of(REPLY_LIKE, REPLY_MENTION);
	private static final List<String> COMMENT_TREE_TYPES = List.of(
			COMMENT_LIKE, COMMENT_MENTION, REPLY_LIKE, REPLY_MENTION);

	private final NotificationMapper notificationMapper;
	private final NotificationSettingService notificationSettingService;
	private final PetService petService;
	private final FileService fileService;
	private final RealtimePush realtimePush;

	public void notifyTagReview(Long userNo, Long requestId, String hashtag, boolean approved) {
		String type = approved ? "TAG_PROFILE_APPROVED" : "TAG_PROFILE_REJECTED";
		notificationMapper.upsert(NotificationUpsert.builder().receiverUserNo(userNo).actorUserNo(userNo)
				.type(type).content(hashtag).actorCount(1).hasComment(0).hasReply(0)
				.groupKey(type + ":" + requestId + ":" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16)).markUnread(1).build());
		pushUnread(userNo);
	}

	public void removeRecentTagReview(Long userNo, Long requestId) {
		if (notificationMapper.deleteRecentTagReview(userNo, requestId) > 0) pushUnread(userNo);
	}

	/** 하단 배지. 읽음 처리하지 않는다 */
	@Transactional(readOnly = true)
	public NotificationUnreadCountResponse unreadCount(UserPrincipal principal) {
		return NotificationUnreadCountResponse.builder()
				.unreadCount(notificationMapper.countUnread(principal.getUserNo()))
				.build();
	}

	/** 알림 페이지 진입. 응답을 만든 뒤 그때까지 쌓인 안 읽음을 읽음으로 바꾼다 */
	@Transactional
	public NotificationListResponse list(UserPrincipal principal, Integer limit) {
		Long userNo = principal.getUserNo();
		List<NotificationListRow> rows = notificationMapper.findByReceiver(userNo, resolveLimit(limit));
		int unreadCount = notificationMapper.countUnread(userNo);
		notificationMapper.markAllRead(userNo);
		pushUnread(userNo);
		return NotificationListResponse.builder()
				.unreadCount(unreadCount)
				.items(toItems(userNo, rows))
				.build();
	}

	@Transactional
	public NotificationItemResponse read(UserPrincipal principal, Long notificationId) {
		Long userNo = principal.getUserNo();
		NotificationListRow row = notificationMapper.findByIdAndReceiver(notificationId, userNo);
		if (row == null) {
			throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
		}
		notificationMapper.markRead(notificationId, userNo);
		row.setIsRead(true);
		pushUnread(userNo);
		return toItems(userNo, List.of(row)).get(0);
	}

	@Transactional
	public void readAll(UserPrincipal principal) {
		notificationMapper.markAllRead(principal.getUserNo());
		pushUnread(principal.getUserNo());
	}

	public void syncFeedLike(Long feedId, boolean markUnread) {
		applySingle(FEED_LIKE, "FEED_LIKE:" + feedId, notificationMapper.findFeedLike(feedId), markUnread);
	}

	/** Service decision notice: self actor, no feed FK, so hidden content cannot hide the notice. */
	public void notifyModeration(Long userNo, Long jobId, String message) {
		writeModerationNotice(userNo, "AI_MODERATION:" + jobId, message);
	}

	public void notifyModerationReview(Long userNo, Long jobId, String message) {
		writeModerationNotice(userNo, "AI_MODERATION:" + jobId + ":" + java.util.UUID.randomUUID().toString().replace("-", ""), message);
	}

	private void writeModerationNotice(Long userNo, String groupKey, String message) {
		notificationMapper.upsert(NotificationUpsert.builder().receiverUserNo(userNo).actorUserNo(userNo)
				.type("AI_MODERATION").content(message).actorCount(1).hasComment(0).hasReply(0)
				.groupKey(groupKey).markUnread(1).build());
		pushUnread(userNo);
	}

	public void syncCommentLike(Long commentId, boolean markUnread) {
		applySingle(COMMENT_LIKE, "COMMENT_LIKE:" + commentId, notificationMapper.findCommentLike(commentId), markUnread);
	}

	public void syncReplyLike(Long replyId, boolean markUnread) {
		applySingle(REPLY_LIKE, "REPLY_LIKE:" + replyId, notificationMapper.findReplyLike(replyId), markUnread);
	}

	public void syncFeedThread(Long feedId, boolean markUnread) {
		applySingle(FEED_COMMENT, "FEED_COMMENT:" + feedId, notificationMapper.findFeedThread(feedId), markUnread);
	}

	public void syncStoryLike(Long storyId, boolean markUnread) {
		applySingle(STORY_LIKE, "STORY_LIKE:" + storyId, notificationMapper.findStoryLike(storyId), markUnread);
	}

	/**
	 * 팔로우·팔로우 요청 알림을 맞춘다. 관계가 있는 동안은 지우지 않는다.
	 * 받은 사람 알림은 요청자면 한 줄. 맞팔이면 상대에게도 한 줄.
	 * @param markReceiverUnread 이번에 그 사람을 팔로우·요청한 경우만 true. 수락·맞팔·해제는 기존 줄을 다시 안 읽음으로 돌리지 않는다
	 */
	public void syncNeighbor(Long neighborId, boolean markReceiverUnread) {
		NeighborNotice notice = notificationMapper.findNeighborNotice(neighborId);
		if (notice == null || notice.getRequesterUserNo() == null || notice.getReceiverUserNo() == null) {
			return;
		}
		if (!"PENDING".equals(notice.getStatus()) && !"ACCEPTED".equals(notice.getStatus())) {
			return;
		}
		boolean followedBack = "ACCEPTED".equals(notice.getStatus()) && Boolean.TRUE.equals(notice.getFollowedBack());
		String requestKey = "NEIGHBOR_REQUEST:" + neighborId;
		String followKey = "NEIGHBOR_FOLLOW:" + neighborId;
		List<Long> before = notificationMapper.findReceivers(null, null, null, neighborId, null, null, null);
		notificationMapper.deleteStaleNeighborNotices(
				neighborId,
				notice.getReceiverUserNo(),
				requestKey,
				notice.getRequesterUserNo(),
				followKey,
				followedBack);
		pushUnread(before);
		save(notice.getReceiverUserNo(), neighborDraft(notice.getRequesterUserNo(), neighborId),
				NEIGHBOR_REQUEST, requestKey, markReceiverUnread);
		if (followedBack) {
			save(notice.getRequesterUserNo(), neighborDraft(notice.getReceiverUserNo(), neighborId),
					NEIGHBOR_REQUEST, followKey, true);
		}
	}

	private static NotificationDraft neighborDraft(Long actorUserNo, Long neighborId) {
		NotificationDraft draft = new NotificationDraft();
		draft.setActorUserNo(actorUserNo);
		draft.setActorCount(1);
		draft.setNeighborId(neighborId);
		return draft;
	}

	public void syncCommentMentions(Long commentId) {
		replaceMentions(
				COMMENT_MENTION,
				"COMMENT_MENTION:" + commentId,
				notificationMapper.findCommentSource(commentId),
				notificationMapper.findCommentMentionReceivers(commentId),
				commentId,
				null);
	}

	public void syncReplyMentions(Long replyId) {
		replaceMentions(
				REPLY_MENTION,
				"REPLY_MENTION:" + replyId,
				notificationMapper.findReplySource(replyId),
				notificationMapper.findReplyMentionReceivers(replyId),
				null,
				replyId);
	}

	public void syncFeedTags(Long feedId) {
		NotificationDraft source = notificationMapper.findFeedSource(feedId);
		String groupKey = "FEED_TAG:" + feedId;
		notificationMapper.deleteByGroupKey(groupKey);
		if (source == null || source.getActorUserNo() == null) {
			return;
		}
		for (Long receiverUserNo : notificationMapper.findFeedTagReceivers(feedId)) {
			save(receiverUserNo, source, FEED_TAG, groupKey, true);
		}
	}

	public void syncStoryTags(Long storyId) {
		NotificationDraft source = notificationMapper.findStorySource(storyId);
		String groupKey = "STORY_TAG:" + storyId;
		notificationMapper.deleteByGroupKey(groupKey);
		if (source == null || source.getActorUserNo() == null) {
			return;
		}
		for (Long receiverUserNo : notificationMapper.findStoryTagReceivers(storyId)) {
			save(receiverUserNo, source, STORY_TAG, groupKey, true);
		}
	}

	public void refreshCommentSnippet(Long commentId) {
		NotificationDraft source = notificationMapper.findCommentSource(commentId);
		if (source == null) {
			return;
		}
		notificationMapper.updateSnippetByComment(commentId, snippet(source.getContent()), COMMENT_DIRECT_TYPES);
	}

	public void refreshReplySnippet(Long replyId) {
		NotificationDraft source = notificationMapper.findReplySource(replyId);
		if (source == null) {
			return;
		}
		notificationMapper.updateSnippetByReply(replyId, snippet(source.getContent()), REPLY_DIRECT_TYPES);
	}

	public void removeByFeed(Long feedId) {
		deleteThenPush(notificationMapper.findReceivers(null, feedId, null, null, null, null, null),
				() -> notificationMapper.deleteByFeedId(feedId));
	}

	public void removeByStory(Long storyId) {
		deleteThenPush(notificationMapper.findReceivers(null, null, storyId, null, null, null, null),
				() -> notificationMapper.deleteByStoryId(storyId));
	}

	public void removeByNeighbor(Long neighborId) {
		deleteThenPush(notificationMapper.findReceivers(null, null, null, neighborId, null, null, null),
				() -> notificationMapper.deleteByNeighborId(neighborId));
	}

	public void removeCommentTree(Long commentId) {
		deleteThenPush(
				notificationMapper.findReceivers(null, null, null, null, commentId, null, COMMENT_TREE_TYPES),
				() -> notificationMapper.deleteByCommentTypes(commentId, COMMENT_TREE_TYPES));
	}

	public void removeReply(Long replyId) {
		deleteThenPush(
				notificationMapper.findReceivers(null, null, null, null, null, replyId, REPLY_DIRECT_TYPES),
				() -> notificationMapper.deleteByReplyTypes(replyId, REPLY_DIRECT_TYPES));
	}

	private void replaceMentions(
			String type,
			String groupKey,
			NotificationDraft source,
			List<Long> receivers,
			Long commentId,
			Long replyId) {
		if (commentId != null) {
			List<String> types = List.of(type);
			deleteThenPush(
					notificationMapper.findReceivers(null, null, null, null, commentId, null, types),
					() -> notificationMapper.deleteByCommentTypes(commentId, types));
		}
		if (replyId != null) {
			List<String> types = List.of(type);
			deleteThenPush(
					notificationMapper.findReceivers(null, null, null, null, null, replyId, types),
					() -> notificationMapper.deleteByReplyTypes(replyId, types));
		}
		if (source == null || source.getActorUserNo() == null || receivers == null) {
			return;
		}
		for (Long receiverUserNo : receivers) {
			save(receiverUserNo, source, type, groupKey, true);
		}
	}

	private void applySingle(String type, String groupKey, NotificationDraft draft, boolean markUnread) {
		if (draft == null || draft.getReceiverUserNo() == null) {
			deleteThenPush(notificationMapper.findReceivers(groupKey, null, null, null, null, null, null),
					() -> notificationMapper.deleteByGroupKey(groupKey));
			return;
		}
		if (draft.getActorUserNo() == null || draft.getActorCount() == null || draft.getActorCount() < 1) {
			notificationMapper.deleteByReceiverGroup(draft.getReceiverUserNo(), groupKey);
			pushUnread(draft.getReceiverUserNo());
			return;
		}
		save(draft.getReceiverUserNo(), draft, type, groupKey, markUnread);
	}

	private void save(
			Long receiverUserNo,
			NotificationDraft draft,
			String type,
			String groupKey,
			boolean markUnread) {
		if (receiverUserNo == null || receiverUserNo.equals(draft.getActorUserNo())) {
			return;
		}
		int hasComment = draft.getHasComment() != null && draft.getHasComment() > 0 ? 1 : 0;
		int hasReply = draft.getHasReply() != null && draft.getHasReply() > 0 ? 1 : 0;
		if (!notificationSettingService.allows(receiverUserNo, type, hasComment, hasReply)) {
			return;
		}
		notificationMapper.upsert(NotificationUpsert.builder()
				.receiverUserNo(receiverUserNo)
				.actorUserNo(draft.getActorUserNo())
				.type(type)
				.feedId(draft.getFeedId())
				.feedCommentId(draft.getFeedCommentId())
				.feedReplyId(draft.getFeedReplyId())
				.storyId(draft.getStoryId())
				.neighborId(draft.getNeighborId())
				.content(snippet(draft.getContent()))
				.actorCount(draft.getActorCount() == null ? 1 : draft.getActorCount())
				.hasComment(hasComment)
				.hasReply(hasReply)
				.groupKey(groupKey)
				.markUnread(markUnread ? 1 : 0)
				.build());
		pushUnread(receiverUserNo);
	}

	private void deleteThenPush(List<Long> receivers, Runnable delete) {
		delete.run();
		pushUnread(receivers);
	}

	private void pushUnread(Long userNo) {
		realtimePush.pushNotificationUnread(userNo);
	}

	private void pushUnread(Collection<Long> userNos) {
		if (userNos == null) {
			return;
		}
		for (Long userNo : userNos) {
			pushUnread(userNo);
		}
	}

	private List<NotificationItemResponse> toItems(Long viewerUserNo, List<NotificationListRow> rows) {
		Set<Long> actorUserNos = new HashSet<>();
		for (NotificationListRow row : rows) {
			if (row.getActorUserNo() != null) {
				actorUserNos.add(row.getActorUserNo());
			}
		}
		Map<Long, Integer> profileIds = petService.representativeProfileFileIds(actorUserNos);
		Set<Long> fileIds = new HashSet<>();
		for (Integer profileFileId : profileIds.values()) {
			if (profileFileId != null) {
				fileIds.add(profileFileId.longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		return rows.stream().map(row -> toItem(viewerUserNo, row, profileIds, files)).toList();
	}

	private NotificationItemResponse toItem(
			Long viewerUserNo,
			NotificationListRow row,
			Map<Long, Integer> profileIds,
			Map<Long, FileSummaryResponse> files) {
		Integer profileFileId = profileIds.get(row.getActorUserNo());
		Long profileId = profileFileId == null ? null : profileFileId.longValue();
		int actorCount = row.getActorCount() == null ? 1 : row.getActorCount();
		boolean hasComment = Boolean.TRUE.equals(row.getHasComment());
		boolean hasReply = Boolean.TRUE.equals(row.getHasReply());
		boolean actorFollows = actorFollowsViewer(viewerUserNo, row);
		boolean following = viewerFollowsActor(viewerUserNo, row);
		return NotificationItemResponse.builder()
				.notificationId(row.getNotificationId())
				.type(row.getType())
				.message(row.getType().startsWith("TAG_PROFILE_") ? "#" + row.getContent() + " 의 태그 요청이 " + ("TAG_PROFILE_APPROVED".equals(row.getType()) ? "수락되었습니다." : "거절되었습니다.")
						: "AI_MODERATION".equals(row.getType()) ? row.getContent()
						: message(row.getType(), row.getActorNickname(), actorCount, hasComment, hasReply, actorFollows))
				.actorUserNo(row.getActorUserNo())
				.actorNickname(row.getActorNickname())
				.actorProfileFileId(profileId)
				.actorProfileFile(profileId == null ? null : files.get(profileId))
				.actorCount(actorCount)
				.hasComment(hasComment)
				.hasReply(hasReply)
				.snippet(row.getContent())
				.feedId(row.getFeedId())
				.feedCommentId(row.getFeedCommentId())
				.feedReplyId(row.getFeedReplyId())
				.storyId(row.getStoryId())
				.neighborId(row.getNeighborId())
				.neighborStatus(row.getNeighborStatus())
				.following(following)
				.read(Boolean.TRUE.equals(row.getIsRead()))
				.updatedAt(row.getUpdatedAt())
				.build();
	}

	static String message(
			String type,
			String nickname,
			int actorCount,
			boolean hasComment,
			boolean hasReply,
			boolean actorFollows) {
		String name = nickname == null ? "회원" : nickname;
		if (NEIGHBOR_REQUEST.equals(type)) {
			if (actorFollows) {
				return name + "님이 팔로우했습니다.";
			}
			return name + "님이 팔로우를 요청했습니다.";
		}
		String who = actorCount > 1
				? name + "님 외 " + (actorCount - 1) + "명이"
				: name + "님이";
		return who + " " + action(type, hasComment, hasReply);
	}

	private static boolean actorFollowsViewer(Long viewerUserNo, NotificationListRow row) {
		return follows(row.getActorUserNo(), viewerUserNo, row);
	}

	private static boolean viewerFollowsActor(Long viewerUserNo, NotificationListRow row) {
		return follows(viewerUserNo, row.getActorUserNo(), row);
	}

	/** fromUserNo가 toUserNo를 팔로우 중이면 true. 요청만 있는 PENDING은 false */
	private static boolean follows(Long fromUserNo, Long toUserNo, NotificationListRow row) {
		if (fromUserNo == null || toUserNo == null || !"ACCEPTED".equals(row.getNeighborStatus())) {
			return false;
		}
		if (fromUserNo.equals(row.getNeighborRequesterUserNo()) && toUserNo.equals(row.getNeighborReceiverUserNo())) {
			return true;
		}
		return fromUserNo.equals(row.getNeighborReceiverUserNo())
				&& toUserNo.equals(row.getNeighborRequesterUserNo())
				&& Boolean.TRUE.equals(row.getFollowedBack());
	}

	private static String action(String type, boolean hasComment, boolean hasReply) {
		return switch (type) {
			case FEED_LIKE -> "게시글에 좋아요를 눌렀습니다.";
			case COMMENT_LIKE -> "댓글에 좋아요를 눌렀습니다.";
			case REPLY_LIKE -> "답글에 좋아요를 눌렀습니다.";
			case FEED_COMMENT -> threadAction(hasComment, hasReply);
			case COMMENT_MENTION -> "댓글에서 회원님을 언급했습니다.";
			case REPLY_MENTION -> "답글에서 회원님을 언급했습니다.";
			case FEED_TAG -> "게시글에서 회원님을 태그했습니다.";
			case STORY_TAG -> "스토리에서 회원님을 태그했습니다.";
			case STORY_LIKE -> "스토리에 공감을 표시했습니다.";
			default -> "알림이 있습니다.";
		};
	}

	private static String threadAction(boolean hasComment, boolean hasReply) {
		if (hasComment && hasReply) {
			return "댓글, 답글을 남겼습니다.";
		}
		if (hasReply) {
			return "댓글에 답글을 남겼습니다.";
		}
		return "게시글에 댓글을 남겼습니다.";
	}

	private static String snippet(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		if (trimmed.length() <= SNIPPET_LENGTH) {
			return trimmed;
		}
		return trimmed.substring(0, SNIPPET_LENGTH) + "…";
	}

	private static int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
