package me._hanho.ultary.domain.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;

/** 경로 원본: springEndpoints.admin / api-memo.md §12 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@PostMapping("/tags/{tagId}/approve")
	public ApiResponse<Void> approveTag(@PathVariable Long tagId) {
		log.info("[approveTag] tagId={}", tagId);
		adminService.approveTag(tagId);
		return ApiResponse.okEmpty("태그 승인 성공");
	}

	@PostMapping("/tags/{tagId}/reject")
	public ApiResponse<Void> rejectTag(@PathVariable Long tagId) {
		log.info("[rejectTag] tagId={}", tagId);
		adminService.rejectTag(tagId);
		return ApiResponse.okEmpty("태그 거절 성공");
	}

	@PostMapping("/users/{userNo}/suspend")
	public ApiResponse<UserSuspendResponse> suspendUser(@PathVariable Long userNo) {
		log.info("[suspendUser] userNo={}", userNo);
		return ApiResponse.ok(adminService.suspendUser(userNo), "회원 정지 성공");
	}

	@PostMapping("/users/{userNo}/unsuspend")
	public ApiResponse<UserSuspendResponse> unsuspendUser(@PathVariable Long userNo) {
		log.info("[unsuspendUser] userNo={}", userNo);
		return ApiResponse.ok(adminService.unsuspendUser(userNo), "회원 정지 해제 성공");
	}

	@GetMapping("/reports")
	public ApiResponse<List<ReportResponse>> reports(
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "30") int limit) {
		return ApiResponse.ok(adminService.listReports(status, limit));
	}

	@PostMapping("/reports/{reportId}/delete-content")
	public ApiResponse<ReportResponse> deleteReportedContent(@PathVariable Long reportId) {
		log.info("[deleteReportedContent] reportId={}", reportId);
		return ApiResponse.ok(adminService.deleteReportedContent(reportId), "신고 대상 삭제 성공");
	}

	@PostMapping("/reports/{reportId}/suspend")
	public ApiResponse<ReportResponse> suspendReportedUser(@PathVariable Long reportId) {
		log.info("[suspendReportedUser] reportId={}", reportId);
		return ApiResponse.ok(adminService.suspendReportedUser(reportId), "신고 회원 정지 성공");
	}

	@PostMapping("/reports/{reportId}/confirm")
	public ApiResponse<ReportResponse> confirmReport(@PathVariable Long reportId) {
		log.info("[confirmReport] reportId={}", reportId);
		return ApiResponse.ok(adminService.confirmReport(reportId), "신고 확인 성공");
	}

	@PostMapping("/reports/{reportId}/reject")
	public ApiResponse<ReportResponse> rejectReport(@PathVariable Long reportId) {
		log.info("[rejectReport] reportId={}", reportId);
		return ApiResponse.ok(adminService.rejectReport(reportId), "신고 거절 성공");
	}

	@PostMapping("/reports/{reportId}/hold")
	public ApiResponse<ReportResponse> holdReport(@PathVariable Long reportId) {
		log.info("[holdReport] reportId={}", reportId);
		return ApiResponse.ok(adminService.holdReport(reportId), "신고 보류 성공");
	}
}
