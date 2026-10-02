package me._hanho.ultary.domain.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.notification.dto.request.UpdateNotificationSettingRequest;
import me._hanho.ultary.domain.notification.dto.response.NotificationSettingResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: api-memo.md §11 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings/notifications")
@RequiredArgsConstructor
public class NotificationSettingController {

	private final NotificationSettingService notificationSettingService;

	@GetMapping
	public ApiResponse<NotificationSettingResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[get] userNo={}", principal.getUserNo());
		return ApiResponse.ok(notificationSettingService.get(principal), "알림 설정 조회 성공");
	}

	@PatchMapping
	public ApiResponse<NotificationSettingResponse> update(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateNotificationSettingRequest request) {
		log.info("[update] userNo={}", principal.getUserNo());
		return ApiResponse.ok(notificationSettingService.update(principal, request), "알림 설정 변경 성공");
	}
}
