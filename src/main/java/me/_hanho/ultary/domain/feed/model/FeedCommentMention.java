package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_comment_mention */
@Getter
@Setter
public class FeedCommentMention {

	private Long feedCommentMentionId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long mentionedUserNo;
	private Long mentionedPetId;
	private LocalDateTime createdAt;
}
