package me._hanho.ultary.domain.tag.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagProfileRequestPageResponse {

	private List<TagProfileRequestResponse> items;
	/** 다음 페이지 커서. 없으면 null */
	private Long nextCursorRequestId;
}
