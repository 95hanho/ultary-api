package me._hanho.ultary.domain.report;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.report.dto.request.CreateReportRequest;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping
	public ApiResponse<ReportResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody CreateReportRequest request) {
		return ApiResponse.ok(reportService.create(principal, request), "신고 접수 성공");
	}

	@DeleteMapping("/{reportId}")
	public ApiResponse<Void> cancel(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long reportId) {
		reportService.cancel(principal, reportId);
		return ApiResponse.okEmpty("신고 취소 성공");
	}
}
