package me._hanho.ultary.domain.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.request.RejectTagProfileRequest;
import me._hanho.ultary.domain.admin.dto.response.AdminMenuCountResponse;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;
import me._hanho.ultary.domain.report.dto.response.ReportPageResponse;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestPageResponse;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestResponse;
import me._hanho.ultary.security.principal.AdminPrincipal;

/** 경로 원본: springEndpoints.admin / api-memo.md §12 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@GetMapping("/menu-counts")
	public ApiResponse<AdminMenuCountResponse> menuCounts() {
		return ApiResponse.ok(adminService.menuCounts(), "관리자 메뉴 건수 조회 성공");
	}

	@GetMapping("/tags/profile-requests")
	public ApiResponse<TagProfileRequestPageResponse> tagProfileRequests(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long cursorRequestId,
			@RequestParam(defaultValue = "30") int limit) {
		log.info("[tagProfileRequests] status={} cursorRequestId={} limit={}", status, cursorRequestId, limit);
		return ApiResponse.ok(
				adminService.listTagProfileRequests(status, cursorRequestId, limit),
				"태그 설명 요청 조회 성공");
	}

	@PostMapping("/tags/{tagId}/approve")
	public ApiResponse<TagProfileRequestResponse> approveTag(
			@AuthenticationPrincipal AdminPrincipal principal,
			@PathVariable Long tagId) {
		log.info("[approveTag] tagId={}", tagId);
		return ApiResponse.ok(adminService.approveTag(principal.getAdminNo(), tagId), "태그 설명 승인 성공");
	}

	@PostMapping("/tags/{tagId}/reject")
	public ApiResponse<TagProfileRequestResponse> rejectTag(
			@AuthenticationPrincipal AdminPrincipal principal,
			@PathVariable Long tagId,
			@Valid @RequestBody(required = false) RejectTagProfileRequest request) {
		log.info("[rejectTag] tagId={}", tagId);
		String rejectMessage = request == null ? null : request.getRejectMessage();
		return ApiResponse.ok(
				adminService.rejectTag(principal.getAdminNo(), tagId, rejectMessage),
				"태그 설명 거절 성공");
	}

	@PostMapping("/tags/{tagId}/pending")
	public ApiResponse<TagProfileRequestResponse> reopenTag(@PathVariable Long tagId) {
		log.info("[reopenTag] tagId={}", tagId);
		return ApiResponse.ok(adminService.reopenTag(tagId), "태그 설명 대기 성공");
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

	@GetMapping("/reports/{reportId}")
	public ApiResponse<ReportResponse> report(@PathVariable Long reportId) {
		return ApiResponse.ok(adminService.getReport(reportId));
	}

	@GetMapping("/reports")
	public ApiResponse<ReportPageResponse> reports(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long cursorReportId,
			@RequestParam(defaultValue = "30") int limit) {
		return ApiResponse.ok(adminService.listReports(status, cursorReportId, limit));
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
