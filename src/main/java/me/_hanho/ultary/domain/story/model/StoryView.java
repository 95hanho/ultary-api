package me._hanho.ultary.domain.story.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_story_view */
@Getter
@Setter
public class StoryView {

	private Long storyViewId;
	private Long storyId;
	private Long viewerUserNo;
	private LocalDateTime viewedAt;
}
