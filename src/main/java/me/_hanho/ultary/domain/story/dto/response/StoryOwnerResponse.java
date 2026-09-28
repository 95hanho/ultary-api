package me._hanho.ultary.domain.story.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/**
 * 메인 상단 스토리 링용.
 * 프로필 사진만 포함하고, 스토리 미디어는 {@code GET /main/stories?userNo=}에서 받는다.
 */
@Getter
@Builder
public class StoryOwnerResponse {

	private Long userNo;
	private String nickname;
	/** 링 아바타용 id. 미등록이면 null */
	private Integer profileFileId;
	/** 유저 프로필. 미등록·삭제 파일이면 null */
	private FileSummaryResponse profileFile;
	/** 활성 스토리 개수 */
	private Integer storyCount;
	/** 내가 아직 안 본 활성 스토리가 하나라도 있으면 true ({@code ultary_story_view} 기준) */
	private Boolean hasUnviewed;
}
