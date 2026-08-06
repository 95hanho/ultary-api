package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_pet */
@Getter
@Setter
public class FeedPet {

	private Long feedPetId;
	private Long feedId;
	private Long petId;
	private Long addedByUserNo;
	/** APPROVED | PENDING | REJECTED */
	private String status;
	private Long approvedByUserNo;
	private LocalDateTime approvedAt;
	private LocalDateTime rejectedAt;
	private Boolean isMain;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
