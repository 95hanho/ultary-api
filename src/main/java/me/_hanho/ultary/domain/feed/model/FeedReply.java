package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_reply */
@Getter
@Setter
public class FeedReply {

	private Long feedReplyId;
	private Long feedCommentId;
	private Long userNo;
	private String content;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
