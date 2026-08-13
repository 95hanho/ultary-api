package me._hanho.ultary.domain.dm;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;

/** 경로 원본: springEndpoints.dm / api-memo.md §9 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DmController {

	private final DmService dmService;

	@GetMapping("/rooms")
	public ApiResponse<Void> rooms() {
		log.info("[rooms]");
		dmService.getRooms();
		return ApiResponse.okEmpty("대화방 목록 조회 성공");
	}

	@PostMapping("/rooms")
	public ApiResponse<Void> createRoom() {
		log.info("[createRoom]");
		dmService.createRoom();
		return ApiResponse.okEmpty("대화방 생성 성공");
	}

	@DeleteMapping("/rooms/{roomId}")
	public ApiResponse<Void> leaveRoom(@PathVariable Long roomId) {
		log.info("[leaveRoom] roomId={}", roomId);
		dmService.leaveRoom(roomId);
		return ApiResponse.okEmpty("대화방 나가기 성공");
	}

	@PostMapping("/rooms/{roomId}/read")
	public ApiResponse<Void> readRoom(@PathVariable Long roomId) {
		log.info("[readRoom] roomId={}", roomId);
		dmService.readRoom(roomId);
		return ApiResponse.okEmpty("대화방 읽음 처리 성공");
	}

	@GetMapping("/rooms/{roomId}/messages")
	public ApiResponse<Void> messages(@PathVariable Long roomId) {
		log.info("[messages] roomId={}", roomId);
		dmService.getMessages(roomId);
		return ApiResponse.okEmpty("메시지 목록 조회 성공");
	}

	@PostMapping("/rooms/{roomId}/messages")
	public ApiResponse<Void> sendMessage(@PathVariable Long roomId) {
		log.info("[sendMessage] roomId={}", roomId);
		dmService.sendMessage(roomId);
		return ApiResponse.okEmpty("메시지 전송 성공");
	}
}
