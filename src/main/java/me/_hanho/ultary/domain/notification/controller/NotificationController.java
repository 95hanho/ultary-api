package me._hanho.ultary.domain.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.notification.service.NotificationService;

/** 경로 원본: springEndpoints.notifications / api-memo.md §10 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	// 알림 목록 조회
	@GetMapping
	public ApiResponse<Void> list() {
		log.info("[list]");
		notificationService.list();
		return ApiResponse.ok();
	}

	// 알림 단건 읽음
	@PatchMapping("/{notificationId}/read")
	public ApiResponse<Void> read(@PathVariable Long notificationId) {
		log.info("[read] notificationId={}", notificationId);
		notificationService.read(notificationId);
		return ApiResponse.ok();
	}

	// 알림 전체 읽음
	@PostMapping("/read-all")
	public ApiResponse<Void> readAll() {
		log.info("[readAll]");
		notificationService.readAll();
		return ApiResponse.ok();
	}
}
