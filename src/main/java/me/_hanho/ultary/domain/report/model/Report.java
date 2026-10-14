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
	/** 목록 조회용. USER 신고의 대상 닉네임 */
	private String targetNickname;
	/** 목록 조회용. FEED·COMMENT·REPLY 본문 */
	private String targetContent;
	/** REQUESTED 신고요청 | CONFIRMED 확인 | DELETED 삭제조치 | REJECTED 거절 | ON_HOLD 보류 | SUSPENDED 정지 */
	private String status;
	private Long adminNo;
	private LocalDateTime processedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
