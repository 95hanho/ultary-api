package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;

@Getter
@Builder
public class MainFeedPageResponse {

	private List<FeedResponse> items;
	/** 다음 페이지 커서. 없으면 null */
	private Long nextCursorFeedId;
}
