package me._hanho.ultary.domain.feed.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_media_mention — 이미지 위 @반려동물 (승인 없음) */
@Getter
@Setter
public class FeedMediaMention {

	private Long feedMediaMentionId;
	private Long feedMediaId;
	private Long petId;
	private BigDecimal posX;
	private BigDecimal posY;
	private Long addedByUserNo;
	private LocalDateTime createdAt;
}
