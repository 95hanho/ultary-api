package me._hanho.ultary.domain.tag.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_tag */
@Getter
@Setter
public class FeedTag {

	private Long feedTagId;
	private Long feedId;
	private Long tagId;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
