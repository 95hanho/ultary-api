package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_store */
@Getter
@Setter
public class FeedStore {

	private Long feedStoreId;
	private Long feedId;
	private Long userNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
