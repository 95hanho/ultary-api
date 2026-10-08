package me._hanho.ultary.domain.report.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

	/** USER, FEED, COMMENT, REPLY */
	private String targetType;

	/** 유저 번호, 게시글 id, 댓글 id, 답글 id */
	private Long targetId;

	/**
	 * SPAM 스팸·광고, ABUSE 욕설·비방, HARASSMENT 괴롭힘, SEXUAL 음란,
	 * VIOLENCE 폭력, HATE 혐오, IMPERSONATION 사칭, PRIVACY 개인정보, OTHER 기타
	 */
	private String reason;
}
