package me._hanho.ultary.domain.story.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/** ultary_story_text */
@Getter
@Setter
public class StoryText {

	private Long storyTextId;
	private Long storyId;
	private String content;
	private Integer fontSize;
	private Boolean bold;
	private Boolean underline;
	private Boolean strikethrough;
	private String color;
	private BigDecimal posX;
	private BigDecimal posY;
	private Integer sortOrder;
}
