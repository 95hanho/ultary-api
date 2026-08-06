package me._hanho.ultary.domain.settings.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.settings.service.SettingsService;

/** 경로 원본: springEndpoints.settings / api-memo.md §11 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final SettingsService settingsService;

	// 설정 조회 (프로필 공개 범위 등)
	@GetMapping
	public ApiResponse<Void> get() {
		log.info("[get]");
		settingsService.get();
		return ApiResponse.ok();
	}

	// 설정 변경
	@PatchMapping
	public ApiResponse<Void> update() {
		log.info("[update]");
		settingsService.update();
		return ApiResponse.ok();
	}
}
