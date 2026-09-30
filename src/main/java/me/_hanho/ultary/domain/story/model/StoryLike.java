package me._hanho.ultary.domain.story.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_story_like */
@Getter
@Setter
public class StoryLike {

	private Long storyLikeId;
	private Long storyId;
	private Long userNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
