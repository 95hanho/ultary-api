package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_like */
@Getter
@Setter
public class FeedLike {

	private Long feedLikeId;
	private Long feedId;
	private Long userNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
