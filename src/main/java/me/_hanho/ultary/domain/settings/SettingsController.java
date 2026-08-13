package me._hanho.ultary.domain.settings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;

/** 경로 원본: springEndpoints.settings / api-memo.md §11 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final SettingsService settingsService;

	@GetMapping
	public ApiResponse<Void> get() {
		log.info("[get]");
		settingsService.get();
		return ApiResponse.okEmpty("설정 조회 성공");
	}

	@PatchMapping
	public ApiResponse<Void> update() {
		log.info("[update]");
		settingsService.update();
		return ApiResponse.okEmpty("설정 변경 성공");
	}
}
