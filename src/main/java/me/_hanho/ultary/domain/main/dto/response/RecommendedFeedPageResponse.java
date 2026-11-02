package me._hanho.ultary.domain.main.dto.response;

import java.util.List;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;

public record RecommendedFeedPageResponse(List<FeedResponse> items, String nextCursor) {}
