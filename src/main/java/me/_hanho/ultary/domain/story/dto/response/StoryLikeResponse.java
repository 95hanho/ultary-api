package me._hanho.ultary.domain.story.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryLikeResponse {

	private Long storyId;
	private int likeCount;
	private boolean likedByMe;
}
