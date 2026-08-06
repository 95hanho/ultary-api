package me._hanho.ultary.domain.report.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.report.service.ReportService;

/** 경로 원본: springEndpoints.users.report / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	// 신고
	@PostMapping
	public ApiResponse<Void> create() {
		log.info("[create]");
		reportService.create();
		return ApiResponse.ok();
	}
}
