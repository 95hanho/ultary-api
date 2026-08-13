package me._hanho.ultary.domain.story.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryOwnerResponse {

	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	/** 활성 스토리 개수 */
	private Integer storyCount;
	/** 내가 아직 안 본 활성 스토리가 하나라도 있으면 true */
	private Boolean hasUnviewed;
}
