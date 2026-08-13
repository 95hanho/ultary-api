package me._hanho.ultary.domain.neighbor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborRelationResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.neighbors / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/neighbors")
@RequiredArgsConstructor
public class NeighborController {

	private final NeighborService neighborService;

	@PostMapping("/{neighborId}/accept")
	public ApiResponse<NeighborRelationResponse> accept(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long neighborId) {
		log.info("[accept] userNo={} neighborId={}", principal.getUserNo(), neighborId);
		return ApiResponse.ok(neighborService.accept(principal, neighborId), "주민 요청 수락 성공");
	}

	@PostMapping("/{neighborId}/reject")
	public ApiResponse<NeighborRelationResponse> reject(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long neighborId) {
		log.info("[reject] userNo={} neighborId={}", principal.getUserNo(), neighborId);
		return ApiResponse.ok(neighborService.reject(principal, neighborId), "주민 요청 거절 성공");
	}

	@DeleteMapping("/{neighborId}")
	public ApiResponse<Void> cancel(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long neighborId) {
		log.info("[cancel] userNo={} neighborId={}", principal.getUserNo(), neighborId);
		neighborService.cancelOrRemove(principal, neighborId);
		return ApiResponse.okEmpty("주민 요청 취소·이웃 해제 성공");
	}
}
