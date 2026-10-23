package me._hanho.ultary.domain.report.dto.response;

import java.util.List;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedCommentResponse;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;

public record ReportFeedPreviewResponse(FeedResponse feed, List<FeedCommentResponse> comments, List<TagResponse> tags) {}
