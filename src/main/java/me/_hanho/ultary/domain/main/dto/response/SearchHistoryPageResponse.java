package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchHistoryPageResponse {

	private List<SearchHistoryItemResponse> items;
	/** 현재 조회자가 볼 수 있는 최근 검색 전체 개수 */
	private long totalCount;
	/** 더보기 커서. 없으면 null */
	private Long nextCursorHistoryId;
}
