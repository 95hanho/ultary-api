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
	private String reporterNickname;
	/** USER | FEED | COMMENT | REPLY */
	private String targetType;
	private Long targetUserNo;
	private Long targetFeedId;
	private Long targetCommentId;
	private Long targetReplyId;
	private String reason;
	/** 관리자 목록·상세. 회원 신고는 그 회원, 글·댓글·답글은 작성자 닉네임 */
	private String targetNickname;
	/** 목록 조회용. FEED·COMMENT·REPLY 본문 */
	private String targetContent;
	/** 대상 회원이 정지인지. 회원은 그 회원, 글·댓글·답글은 작성자 */
	private boolean targetSuspended;
	/** REQUESTED 신고요청 | CONFIRMED 확인 | DELETED 삭제조치 | REJECTED 거절 | SUSPENDED 정지 */
	private String status;
	private Long adminNo;
	private LocalDateTime processedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
