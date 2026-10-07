package me._hanho.ultary.domain.pet.model;

import lombok.Getter;
import lombok.Setter;

/** 유저의 펫 멘션. mentionId에는 @를 붙이지 않는다 */
@Getter
@Setter
public class PetMention {

	private Long userNo;
	private String mentionId;
}
