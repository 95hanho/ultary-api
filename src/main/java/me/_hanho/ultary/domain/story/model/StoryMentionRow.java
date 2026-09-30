package me._hanho.ultary.domain.story.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/** ultary_story_mention + 펫 mention_id·이름 */
@Getter
@Setter
public class StoryMentionRow {

	private Long storyMentionId;
	private Long storyId;
	private Long petId;
	private String mentionId;
	private String petName;
	private BigDecimal posX;
	private BigDecimal posY;
	private Integer sortOrder;
	private Long addedByUserNo;
}
