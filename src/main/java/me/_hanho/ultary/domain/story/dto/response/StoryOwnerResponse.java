package me._hanho.ultary.domain.story.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 메인 상단 스토리 링용.
 * 미디어/프로필 URL은 포함하지 않음 — 눌렀을 때 {@code GET /main/stories?userNo=} 사용.
 */
@Getter
@Builder
public class StoryOwnerResponse {

	private Long userNo;
	private String nickname;
	/** 링 아바타용 id (선택). URL은 별도 조회 */
	private Integer profileFileId;
	/** 활성 스토리 개수 */
	private Integer storyCount;
	/** 내가 아직 안 본 활성 스토리가 하나라도 있으면 true ({@code ultary_story_view} 기준) */
	private Boolean hasUnviewed;
}
