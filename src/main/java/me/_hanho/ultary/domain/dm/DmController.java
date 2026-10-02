package me._hanho.ultary.domain.dm;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.dm.dto.request.CreateDmRoomRequest;
import me._hanho.ultary.domain.dm.dto.request.SendDmMessageRequest;
import me._hanho.ultary.domain.dm.dto.response.DmMessageListResponse;
import me._hanho.ultary.domain.dm.dto.response.DmMessageResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomItemResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomListResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: api-memo.md §9 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dm/rooms")
@RequiredArgsConstructor
public class DmController {

	private final DmService dmService;

	@GetMapping
	public ApiResponse<DmRoomListResponse> list(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[list] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(dmService.listRooms(principal, limit), "대화방 목록 조회 성공");
	}

	@PostMapping
	public ApiResponse<DmRoomItemResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody CreateDmRoomRequest request) {
		log.info("[create] userNo={} target={}", principal.getUserNo(),
				request == null ? null : request.getTargetUserNo());
		return ApiResponse.ok(dmService.createRoom(principal, request), "대화방 생성 성공");
	}

	@DeleteMapping("/{roomId}")
	public ApiResponse<Void> leave(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long roomId) {
		log.info("[leave] userNo={} roomId={}", principal.getUserNo(), roomId);
		dmService.leave(principal, roomId);
		return ApiResponse.okEmpty("대화방 나가기 성공");
	}

	@PostMapping("/{roomId}/read")
	public ApiResponse<Void> read(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long roomId) {
		log.info("[read] userNo={} roomId={}", principal.getUserNo(), roomId);
		dmService.read(principal, roomId);
		return ApiResponse.okEmpty("대화 읽음 처리 성공");
	}

	@GetMapping("/{roomId}/messages")
	public ApiResponse<DmMessageListResponse> messages(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long roomId,
			@RequestParam(required = false) Long beforeMessageId,
			@RequestParam(required = false) Integer limit) {
		log.info("[messages] userNo={} roomId={} before={}", principal.getUserNo(), roomId, beforeMessageId);
		return ApiResponse.ok(
				dmService.messages(principal, roomId, beforeMessageId, limit),
				"메시지 조회 성공");
	}

	@PostMapping("/{roomId}/messages")
	public ApiResponse<DmMessageResponse> send(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long roomId,
			@RequestBody SendDmMessageRequest request) {
		log.info("[send] userNo={} roomId={}", principal.getUserNo(), roomId);
		return ApiResponse.ok(dmService.send(principal, roomId, request), "메시지 전송 성공");
	}
}
