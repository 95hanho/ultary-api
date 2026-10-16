package me._hanho.ultary.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 관리자 메뉴 뱃지. 목록은 포함하지 않는다 */
@Getter
@Builder
public class AdminMenuCountResponse {

	/** 상업 태그 설명 요청 중 PENDING */
	private int pendingTagRequestCount;
	/** 신고 중 REQUESTED·CONFIRMED 합 */
	private int requestedReportCount;
}
