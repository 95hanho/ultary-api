package me._hanho.ultary.domain.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationItemResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationListResponse;
import me._hanho.ultary.domain.notification.dto.response.NotificationUnreadCountResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.notifications / api-memo.md §10 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	/** 하단 공통 배지. /auth/me 에 넣지 않는다. 나중에 웹소켓이 이 숫자를 밀어 준다 */
	@GetMapping("/unread-count")
	public ApiResponse<NotificationUnreadCountResponse> unreadCount(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[unreadCount] userNo={}", principal.getUserNo());
		return ApiResponse.ok(notificationService.unreadCount(principal), "안 읽은 알림 수 조회 성공");
	}

	@GetMapping
	public ApiResponse<NotificationListResponse> list(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[list] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(notificationService.list(principal, limit), "알림 목록 조회 성공");
	}

	@PatchMapping("/{notificationId}/read")
	public ApiResponse<NotificationItemResponse> read(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long notificationId) {
		log.info("[read] userNo={} notificationId={}", principal.getUserNo(), notificationId);
		return ApiResponse.ok(notificationService.read(principal, notificationId), "알림 읽음 처리 성공");
	}

	@PostMapping("/read-all")
	public ApiResponse<Void> readAll(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[readAll] userNo={}", principal.getUserNo());
		notificationService.readAll(principal);
		return ApiResponse.okEmpty("알림 전체 읽음 성공");
	}
}
