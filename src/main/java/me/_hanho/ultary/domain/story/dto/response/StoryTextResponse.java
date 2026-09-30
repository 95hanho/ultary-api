package me._hanho.ultary.domain.story.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryTextResponse {

	private Long storyTextId;
	private String content;
	private Integer fontSize;
	private Boolean bold;
	private Boolean underline;
	private Boolean strikethrough;
	private String color;
	private BigDecimal posX;
	private BigDecimal posY;
}
