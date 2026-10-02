package me._hanho.ultary.domain.settings;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.domain.settings.dto.response.PrivacyResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: api-memo.md §11 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings/privacy")
@RequiredArgsConstructor
public class PrivacyController {

	private final PrivacyService privacyService;

	@GetMapping
	public ApiResponse<PrivacyResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[get] userNo={}", principal.getUserNo());
		return ApiResponse.ok(privacyService.get(principal), "공개 범위 조회 성공");
	}

	@PatchMapping
	public ApiResponse<PrivacyResponse> update(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdatePrivacyRequest request) {
		log.info("[update] userNo={}", principal.getUserNo());
		return ApiResponse.ok(privacyService.update(principal, request), "공개 범위 변경 성공");
	}
}
