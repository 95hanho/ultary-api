package me._hanho.ultary.domain.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborListItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborRelationResponse;
import me._hanho.ultary.domain.neighbor.dto.response.UserUltaryResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.users / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final NeighborService neighborService;

	@GetMapping("/{userNo}/ultary")
	public ApiResponse<UserUltaryResponse> ultary(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[ultary] me={} userNo={}", principal.getUserNo(), userNo);
		return ApiResponse.ok(neighborService.getUltary(principal, userNo), "유저 울타리 조회 성공");
	}

	@GetMapping("/{userNo}/neighbors")
	public ApiResponse<List<NeighborListItemResponse>> neighbors(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Integer limit) {
		log.info("[neighbors] me={} userNo={} type={}", principal.getUserNo(), userNo, type);
		return ApiResponse.ok(
				neighborService.listNeighbors(principal, userNo, type, limit),
				"주민·이웃 목록 조회 성공");
	}

	@PostMapping("/{userNo}/neighbors/request")
	public ApiResponse<NeighborRelationResponse> neighborRequest(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[neighborRequest] me={} userNo={}", principal.getUserNo(), userNo);
		return ApiResponse.ok(neighborService.request(principal, userNo), "주민 요청 성공");
	}

	@PostMapping("/{userNo}/block")
	public ApiResponse<Void> block(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[block] me={} userNo={}", principal.getUserNo(), userNo);
		neighborService.block(principal, userNo);
		return ApiResponse.okEmpty("유저 차단 성공");
	}

	@DeleteMapping("/{userNo}/block")
	public ApiResponse<Void> unblock(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[unblock] me={} userNo={}", principal.getUserNo(), userNo);
		neighborService.unblock(principal, userNo);
		return ApiResponse.okEmpty("유저 차단 해제 성공");
	}
}
