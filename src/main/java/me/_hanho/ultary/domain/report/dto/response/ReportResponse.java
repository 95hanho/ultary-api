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
	/** 대상 회원 닉네임. 회원 신고는 그 회원, 글·댓글·답글은 작성자. 관리자 목록·상세에서 채운다 */
	private String targetNickname;
	/** 게시글·댓글·답글 본문. 10자, 넘으면 뒤에 ... 목록에서만 채운다 */
	private String targetContent;
	/** 대상 회원이 정지인지. 회원 신고는 그 회원, 글·댓글·답글은 작성자. withdrawal_status=SUSPENDED일 때만 true */
	private boolean targetSuspended;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime processedAt;
}
