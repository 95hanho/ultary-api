package me._hanho.ultary.domain.test.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryViewResetResponse {

	private Long viewerUserNo;
	/** 삭제된 ultary_story_view 행 수 */
	private int deletedCount;
}
