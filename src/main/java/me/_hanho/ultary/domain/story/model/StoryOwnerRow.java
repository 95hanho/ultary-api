package me._hanho.ultary.domain.story.model;

import lombok.Getter;
import lombok.Setter;

/** 스토리 보유 유저 조회 row */
@Getter
@Setter
public class StoryOwnerRow {

	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private Integer storyCount;
}
