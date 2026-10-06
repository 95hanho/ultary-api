package me._hanho.ultary.domain.ws;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.ws.dto.WsTicketResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: api-memo.md 웹소켓 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ws")
@RequiredArgsConstructor
public class WsController {

	private final WsTicketService ticketService;

	@PostMapping("/ticket")
	public ApiResponse<WsTicketResponse> ticket(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[wsTicket] userNo={}", principal.getUserNo());
		return ApiResponse.ok(ticketService.issue(principal.getUserNo()), "웹소켓 입장 토큰 발급 성공");
	}
}
