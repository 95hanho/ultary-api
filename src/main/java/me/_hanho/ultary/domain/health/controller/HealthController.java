package me._hanho.ultary.domain.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.health.service.HealthService;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

	private final HealthService healthService;

	// 서버 생존 확인
	@GetMapping("/health")
	public ApiResponse<String> health() {
		log.info("[health]");
		return ApiResponse.ok("OK");
	}

	// DB 연결 확인
	@GetMapping("/health/db")
	public ApiResponse<String> healthDb() {
		log.info("[healthDb]");
		return ApiResponse.ok(healthService.checkDbConnection());
	}
}
