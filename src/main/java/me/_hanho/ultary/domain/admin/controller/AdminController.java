package me._hanho.ultary.domain.admin.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.service.AdminService;

/** 경로 원본: springEndpoints.admin / api-memo.md §12 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	// 태그 승인
	@PostMapping("/tags/{tagId}/approve")
	public ApiResponse<Void> approveTag(@PathVariable Long tagId) {
		log.info("[approveTag] tagId={}", tagId);
		adminService.approveTag(tagId);
		return ApiResponse.ok();
	}

	// 태그 거절
	@PostMapping("/tags/{tagId}/reject")
	public ApiResponse<Void> rejectTag(@PathVariable Long tagId) {
		log.info("[rejectTag] tagId={}", tagId);
		adminService.rejectTag(tagId);
		return ApiResponse.ok();
	}
}
