package me._hanho.ultary.domain.report.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_report */
@Getter
@Setter
public class Report {

	private Long reportId;
	private Long reporterUserNo;
	/** USER | PET | FEED | COMMENT | REPLY */
	private String targetType;
	private Long targetUserNo;
	private Long targetPetId;
	private Long targetFeedId;
	private Long targetCommentId;
	private Long targetReplyId;
	private String reason;
	/** PENDING | REVIEWING | RESOLVED | REJECTED */
	private String status;
	private Long adminNo;
	private LocalDateTime processedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
