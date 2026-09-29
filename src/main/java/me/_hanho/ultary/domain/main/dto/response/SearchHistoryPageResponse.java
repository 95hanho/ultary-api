package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchHistoryPageResponse {

	private List<SearchHistoryItemResponse> items;
	/** 더보기 커서. 없으면 null */
	private Long nextCursorHistoryId;
}
