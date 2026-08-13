package me._hanho.ultary.domain.feed.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedShareResponse {

	private Long feedId;
	/** FE/BFF 경로 힌트 (예: /feeds/12) */
	private String path;
}
