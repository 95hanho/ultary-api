package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_image */
@Getter
@Setter
public class FeedImage {

	private Long feedImageId;
	private Long feedId;
	private Long fileId;
	private Integer sortOrder;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
