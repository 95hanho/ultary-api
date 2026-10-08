package me._hanho.ultary.domain.report.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 조회한 사람의 해당 대상 신고. 없으면 null */
@Getter
@Builder
public class MyReportResponse {

	private Long reportId;
	private String status;
}
