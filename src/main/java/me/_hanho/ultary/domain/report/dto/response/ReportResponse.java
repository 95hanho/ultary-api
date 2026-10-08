package me._hanho.ultary.domain.report.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {

	private Long reportId;
	private Long reporterUserNo;
	private String targetType;
	private Long targetUserNo;
	private Long targetFeedId;
	private Long targetCommentId;
	private Long targetReplyId;
	private String reason;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime processedAt;
}
