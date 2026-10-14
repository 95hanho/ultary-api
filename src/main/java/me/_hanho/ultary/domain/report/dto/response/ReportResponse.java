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
	/** 회원 신고의 대상 닉네임. 목록에서만 채운다 */
	private String targetNickname;
	/** 게시글·댓글·답글 본문. 10자, 넘으면 뒤에 ... 목록에서만 채운다 */
	private String targetContent;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime processedAt;
}
