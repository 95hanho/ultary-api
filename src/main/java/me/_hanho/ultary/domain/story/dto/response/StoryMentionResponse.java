package me._hanho.ultary.domain.story.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryMentionResponse {

	private Long storyMentionId;
	private Long petId;
	@com.fasterxml.jackson.annotation.JsonIgnore
	private String mentionId;
	private String petName;
	private BigDecimal posX;
	private BigDecimal posY;
}
