package me._hanho.ultary.domain.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.health.service.HealthService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

	private final HealthService healthService;

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("OK");
	}

	@GetMapping("/health/db")
	public ApiResponse<String> healthDb() {
		return ApiResponse.ok(healthService.checkDbConnection());
	}
}
