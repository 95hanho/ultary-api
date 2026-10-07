package me._hanho.ultary.domain.admin;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;

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
}
