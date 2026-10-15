package me._hanho.ultary.domain.report.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportPageResponse {

	private List<ReportResponse> items;
	/** 다음 페이지 커서. 없으면 null */
	private Long nextCursorReportId;
	/** status와 같은 조건의 전체 건수. 커서와 limit는 빼지 않는다 */
	private int totalCount;
}
