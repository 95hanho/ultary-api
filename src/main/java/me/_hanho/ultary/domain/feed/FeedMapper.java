package me._hanho.ultary.domain.feed;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.feed.model.FeedComment;
import me._hanho.ultary.domain.feed.model.FeedCommentMention;
import me._hanho.ultary.domain.feed.model.FeedLike;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.feed.model.FeedMediaMention;
import me._hanho.ultary.domain.feed.model.FeedPet;
import me._hanho.ultary.domain.feed.model.FeedReply;
import me._hanho.ultary.domain.feed.model.FeedStore;

@Mapper
public interface FeedMapper {

	int insertFeed(Feed feed);

	Feed findActiveByFeedId(@Param("feedId") Long feedId);

	List<Feed> findActiveByUserNo(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	int countActiveByUserNo(@Param("userNo") Long userNo);

	List<Feed> findSavedByUserNo(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	List<Feed> findTaggedByUserNo(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	/** 본인 + 주민(requester ACCEPTED) 타임라인. cursor 이후(더 오래된) limit건 */
	List<Feed> findMainTimeline(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
			@Param("cursorFeedId") Long cursorFeedId,
			@Param("limit") int limit);

	List<Feed> searchVisible(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);

	int updateFeed(Feed feed);

	int softDeleteFeed(
			@Param("feedId") Long feedId,
			@Param("deletedByUserNo") Long deletedByUserNo);

	int countCollaboratorOwnedByUser(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int insertMedia(FeedMedia media);

	List<FeedMedia> findMediaByFeedId(@Param("feedId") Long feedId);

	int insertMediaMention(FeedMediaMention mention);

	List<FeedMediaMention> findMentionsByFeedMediaId(@Param("feedMediaId") Long feedMediaId);

	int insertFeedPet(FeedPet feedPet);

	List<FeedPet> findPetsByFeedId(@Param("feedId") Long feedId);

	int insertFeedTag(
			@Param("feedId") Long feedId,
			@Param("tagId") Long tagId);

	List<Long> findTagIdsByFeedId(@Param("feedId") Long feedId);

	FeedLike findLike(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int insertLike(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int restoreLike(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int softDeleteLike(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int adjustLikeCount(
			@Param("feedId") Long feedId,
			@Param("delta") int delta);

	List<FeedLike> findActiveLikers(
			@Param("feedId") Long feedId,
			@Param("limit") int limit);

	FeedStore findStore(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int insertStore(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int restoreStore(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int softDeleteStore(
			@Param("feedId") Long feedId,
			@Param("userNo") Long userNo);

	int adjustStoreCount(
			@Param("feedId") Long feedId,
			@Param("delta") int delta);

	int insertComment(FeedComment comment);

	FeedComment findActiveComment(
			@Param("feedId") Long feedId,
			@Param("commentId") Long commentId);

	List<FeedComment> findActiveComments(
			@Param("feedId") Long feedId,
			@Param("limit") int limit);

	/** 활성 댓글 수 + 그 댓글들의 활성 답글 수 */
	int countActiveCommentsAndReplies(@Param("feedId") Long feedId);

	int countActiveRepliesByCommentId(@Param("commentId") Long commentId);

	int updateCommentContent(
			@Param("commentId") Long commentId,
			@Param("userNo") Long userNo,
			@Param("content") String content);

	int softDeleteComment(
			@Param("commentId") Long commentId,
			@Param("userNo") Long userNo);

	int softDeleteCommentByFeedAuthor(
			@Param("commentId") Long commentId,
			@Param("feedId") Long feedId,
			@Param("feedAuthorUserNo") Long feedAuthorUserNo);

	int adjustCommentCount(
			@Param("feedId") Long feedId,
			@Param("delta") int delta);

	int insertReply(FeedReply reply);

	FeedReply findActiveReply(
			@Param("commentId") Long commentId,
			@Param("replyId") Long replyId);

	List<FeedReply> findActiveReplies(
			@Param("commentId") Long commentId,
			@Param("limit") int limit);

	int updateReplyContent(
			@Param("replyId") Long replyId,
			@Param("userNo") Long userNo,
			@Param("content") String content);

	int softDeleteReply(
			@Param("replyId") Long replyId,
			@Param("userNo") Long userNo);

	int softDeleteReplyByFeedAuthor(
			@Param("replyId") Long replyId,
			@Param("commentId") Long commentId,
			@Param("feedAuthorUserNo") Long feedAuthorUserNo);

	int insertCommentMention(FeedCommentMention mention);

	List<FeedCommentMention> findMentionsByCommentId(@Param("commentId") Long commentId);

	List<FeedCommentMention> findMentionsByReplyId(@Param("replyId") Long replyId);
}
