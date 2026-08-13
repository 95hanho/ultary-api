package me._hanho.ultary.domain.user;

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

/** 경로 원본: springEndpoints.users / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserRelationService userRelationService;

	@GetMapping("/{userNo}/ultary")
	public ApiResponse<Void> ultary(@PathVariable Long userNo) {
		log.info("[ultary] userNo={}", userNo);
		userRelationService.getUltary(userNo);
		return ApiResponse.okEmpty("유저 울타리 조회 성공");
	}

	@GetMapping("/{userNo}/neighbors")
	public ApiResponse<Void> neighbors(
			@PathVariable Long userNo,
			@RequestParam(required = false) String type) {
		log.info("[neighbors] userNo={}, type={}", userNo, type);
		userRelationService.getNeighbors(userNo, type);
		return ApiResponse.okEmpty("주민·이웃 목록 조회 성공");
	}

	@PostMapping("/{userNo}/neighbors/request")
	public ApiResponse<Void> neighborRequest(@PathVariable Long userNo) {
		log.info("[neighborRequest] userNo={}", userNo);
		userRelationService.requestNeighbor(userNo);
		return ApiResponse.okEmpty("주민 요청 성공");
	}

	@PostMapping("/{userNo}/block")
	public ApiResponse<Void> block(@PathVariable Long userNo) {
		log.info("[block] userNo={}", userNo);
		userRelationService.block(userNo);
		return ApiResponse.okEmpty("유저 차단 성공");
	}

	@DeleteMapping("/{userNo}/block")
	public ApiResponse<Void> unblock(@PathVariable Long userNo) {
		log.info("[unblock] userNo={}", userNo);
		userRelationService.unblock(userNo);
		return ApiResponse.okEmpty("유저 차단 해제 성공");
	}
}
