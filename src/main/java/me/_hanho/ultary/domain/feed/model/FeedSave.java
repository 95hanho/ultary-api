package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_save. 나만 보는 저장 */
@Getter
@Setter
public class FeedSave {

	private Long feedSaveId;
	private Long feedId;
	private Long userNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
