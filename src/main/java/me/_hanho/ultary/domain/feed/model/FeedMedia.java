package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_media — 피드 캐러셀 슬롯 (사진/짧은 영상) */
@Getter
@Setter
public class FeedMedia {

	private Long feedMediaId;
	private Long feedId;
	private Long fileId;
	/** IMAGE | VIDEO */
	private String mediaType;
	private Long thumbnailFileId;
	private Integer durationSec;
	private Integer sortOrder;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
