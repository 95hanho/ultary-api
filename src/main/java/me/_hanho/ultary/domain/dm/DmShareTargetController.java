package me._hanho.ultary.domain.dm;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.dm.dto.response.DmShareTargetListResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 피드 공유 «메시지로 보내기» 대상. 경로 원본: api-memo.md §9 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dm/share-targets")
@RequiredArgsConstructor
public class DmShareTargetController {

	private final DmService dmService;

	@GetMapping
	public ApiResponse<DmShareTargetListResponse> list(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String priorityOrder,
			@RequestParam(defaultValue = "SHARE") String mode,
            @RequestParam(required = false) Long feedId) {
		log.info("[shareTargets] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(dmService.shareTargets(principal, limit, q, priorityOrder, mode, feedId), "공유 대상 조회 성공");
	}
}
