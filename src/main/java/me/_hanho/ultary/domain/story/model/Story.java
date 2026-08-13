package me._hanho.ultary.domain.story.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_story */
@Getter
@Setter
public class Story {

	private Long storyId;
	private Long userNo;
	private Long fileId;
	/** IMAGE | VIDEO */
	private String mediaType;
	private Long thumbnailFileId;
	private Integer durationSec;
	private String caption;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
