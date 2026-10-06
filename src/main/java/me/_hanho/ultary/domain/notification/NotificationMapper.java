package me._hanho.ultary.domain.notification;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.notification.model.NotificationDraft;
import me._hanho.ultary.domain.notification.model.NotificationListRow;
import me._hanho.ultary.domain.notification.model.NotificationUpsert;

@Mapper
public interface NotificationMapper {

	int upsert(NotificationUpsert row);

	int deleteByReceiverGroup(
			@Param("receiverUserNo") Long receiverUserNo,
			@Param("groupKey") String groupKey);

	int deleteByGroupKey(@Param("groupKey") String groupKey);

	int deleteByFeedId(@Param("feedId") Long feedId);

	int deleteByStoryId(@Param("storyId") Long storyId);

	int deleteByNeighborId(@Param("neighborId") Long neighborId);

	int deleteByCommentTypes(
			@Param("feedCommentId") Long feedCommentId,
			@Param("types") List<String> types);

	int deleteByReplyTypes(
			@Param("feedReplyId") Long feedReplyId,
			@Param("types") List<String> types);

	/** 지우기 전에 배지를 갱신할 수신자. 조건은 하나만 쓴다 */
	List<Long> findReceivers(
			@Param("groupKey") String groupKey,
			@Param("feedId") Long feedId,
			@Param("storyId") Long storyId,
			@Param("neighborId") Long neighborId,
			@Param("feedCommentId") Long feedCommentId,
			@Param("feedReplyId") Long feedReplyId,
			@Param("types") List<String> types);

	int updateSnippetByComment(
			@Param("feedCommentId") Long feedCommentId,
			@Param("content") String content,
			@Param("types") List<String> types);

	int updateSnippetByReply(
			@Param("feedReplyId") Long feedReplyId,
			@Param("content") String content,
			@Param("types") List<String> types);

	NotificationDraft findFeedLike(@Param("feedId") Long feedId);

	NotificationDraft findCommentLike(@Param("commentId") Long commentId);

	NotificationDraft findReplyLike(@Param("replyId") Long replyId);

	NotificationDraft findFeedThread(@Param("feedId") Long feedId);

	NotificationDraft findCommentSource(@Param("commentId") Long commentId);

	NotificationDraft findReplySource(@Param("replyId") Long replyId);

	NotificationDraft findFeedSource(@Param("feedId") Long feedId);

	NotificationDraft findStorySource(@Param("storyId") Long storyId);

	NotificationDraft findStoryLike(@Param("storyId") Long storyId);

	NotificationDraft findNeighborRequest(@Param("neighborId") Long neighborId);

	List<Long> findFeedTagReceivers(@Param("feedId") Long feedId);

	List<Long> findStoryTagReceivers(@Param("storyId") Long storyId);

	List<Long> findCommentMentionReceivers(@Param("commentId") Long commentId);

	List<Long> findReplyMentionReceivers(@Param("replyId") Long replyId);

	List<NotificationListRow> findByReceiver(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	int countUnread(@Param("userNo") Long userNo);

	NotificationListRow findByIdAndReceiver(
			@Param("notificationId") Long notificationId,
			@Param("userNo") Long userNo);

	int markRead(
			@Param("notificationId") Long notificationId,
			@Param("userNo") Long userNo);

	int markAllRead(@Param("userNo") Long userNo);
}
