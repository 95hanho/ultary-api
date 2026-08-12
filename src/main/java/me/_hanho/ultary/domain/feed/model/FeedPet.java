package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed_pet — 승인 없음. COLLABORATOR면 멘션된 피드·삭제 권한 */
@Getter
@Setter
public class FeedPet {

	private Long feedPetId;
	private Long feedId;
	private Long petId;
	private Long addedByUserNo;
	/** TAGGED | COLLABORATOR */
	private String role;
	private Boolean isMain;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
