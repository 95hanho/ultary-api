package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_pin. 울타리에 고정한 글 */
@Getter
@Setter
public class FeedPin {

	private Long feedPinId;
	private Long feedId;
	private Long userNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
