package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_comment */
@Getter
@Setter
public class FeedComment {

	private Long feedCommentId;
	private Long feedId;
	private Long userNo;
	private String content;
	private Integer likeCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;

	/** 조인 조회용 */
	private String authorNickname;
	/** 조인 조회용. 미등록이면 null */
	private Integer authorProfileFileId;
}
