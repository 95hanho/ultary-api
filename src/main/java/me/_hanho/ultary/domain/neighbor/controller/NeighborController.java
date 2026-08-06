package me._hanho.ultary.domain.neighbor.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.neighbor.service.NeighborService;

/** 경로 원본: springEndpoints.users neighbor* / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/neighbors")
@RequiredArgsConstructor
public class NeighborController {

	private final NeighborService neighborService;

	// 주민 요청 수락
	@PostMapping("/{neighborId}/accept")
	public ApiResponse<Void> accept(@PathVariable Long neighborId) {
		log.info("[accept] neighborId={}", neighborId);
		neighborService.accept(neighborId);
		return ApiResponse.ok();
	}

	// 주민 요청 거절
	@PostMapping("/{neighborId}/reject")
	public ApiResponse<Void> reject(@PathVariable Long neighborId) {
		log.info("[reject] neighborId={}", neighborId);
		neighborService.reject(neighborId);
		return ApiResponse.ok();
	}

	// 주민 요청 취소 · 이웃 해제
	@DeleteMapping("/{neighborId}")
	public ApiResponse<Void> cancel(@PathVariable Long neighborId) {
		log.info("[cancel] neighborId={}", neighborId);
		neighborService.cancel(neighborId);
		return ApiResponse.ok();
	}
}
