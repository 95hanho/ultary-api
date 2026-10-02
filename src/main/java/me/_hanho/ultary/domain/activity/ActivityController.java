package me._hanho.ultary.domain.activity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.activity.dto.response.ActivityListResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: api-memo.md §11 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings/activities")
@RequiredArgsConstructor
public class ActivityController {

	private final ActivityService activityService;

	@GetMapping
	public ApiResponse<ActivityListResponse> list(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[list] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(activityService.list(principal, limit), "내 활동 조회 성공");
	}
}
