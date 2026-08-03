package me._hanho.ultary.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me._hanho.ultary.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.ok("OK");
	}
}
