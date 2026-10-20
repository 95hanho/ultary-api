package me._hanho.ultary.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 관리자 메뉴 뱃지. 목록은 포함하지 않는다 */
@Getter
@Builder
public class AdminMenuCountResponse {

	/** 상업 태그 설명 요청 중 PENDING */
	private int pendingTagRequestCount;
	/** 신고 중 REQUESTED·CONFIRMED 합. 대상 회원/작성자가 정지된 건 제외 */
	private int requestedReportCount;
	/** AI 호출 로그 중 FAILED 건수 (재시도별 개별 기록) */
	private int failedAiRequestCount;
}
