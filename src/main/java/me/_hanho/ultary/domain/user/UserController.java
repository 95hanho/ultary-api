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
import me._hanho.ultary.domain.myultary.MyUltaryService;
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.neighbor.dto.response.BlockedUserItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborListItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborRelationResponse;
import me._hanho.ultary.domain.neighbor.dto.response.UserUltaryResponse;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.users / api-memo.md §7 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final NeighborService neighborService;
	private final PetService petService;
	private final MyUltaryService myUltaryService;

	@GetMapping("/{userNo}/ultary")
	public ApiResponse<UserUltaryResponse> ultary(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[ultary] me={} userNo={}", principal.getUserNo(), userNo);
		return ApiResponse.ok(neighborService.getUltary(principal, userNo), "유저 울타리 조회 성공");
	}

	/** 그 유저의 펫. 정렬은 GET /pets 와 같다 */
	@GetMapping("/{userNo}/pets")
	public ApiResponse<List<PetResponse>> pets(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo) {
		log.info("[pets] me={} userNo={}", principal.getUserNo(), userNo);
		neighborService.assertCanViewUltary(principal, userNo);
		return ApiResponse.ok(petService.listByOwner(userNo), "반려동물 목록 조회 성공");
	}

	/** 그 유저의 게시글 그리드. limit 또는 size. 본인이면 내 그리드와 같다 */
	@GetMapping("/{userNo}/feeds")
	public ApiResponse<List<FeedGridItemResponse>> feeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Integer size) {
		log.info("[feeds] me={} userNo={} limit={} size={}", principal.getUserNo(), userNo, limit, size);
		neighborService.assertCanViewUltary(principal, userNo);
		Integer pageSize = size != null ? size : limit;
		return ApiResponse.ok(
				myUltaryService.getFeedsOf(principal, userNo, pageSize),
				"유저 게시글 목록 조회 성공");
	}

	/** 그 유저가 울타리에 고정한 글. 본인이면 내 고정 목록과 같다 */
	@GetMapping("/{userNo}/pinned-feeds")
	public ApiResponse<List<FeedGridItemResponse>> pinnedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Integer size) {
		log.info("[pinnedFeeds] me={} userNo={} limit={} size={}", principal.getUserNo(), userNo, limit, size);
		neighborService.assertCanViewUltary(principal, userNo);
		Integer pageSize = size != null ? size : limit;
		return ApiResponse.ok(
				myUltaryService.getPinnedFeedsOf(principal, userNo, pageSize),
				"유저 고정 게시글 조회 성공");
	}

	/** 그 유저가 태그된 글. 본인이면 내 태그 목록과 같다 */
	@GetMapping("/{userNo}/tagged-feeds")
	public ApiResponse<List<FeedGridItemResponse>> taggedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userNo,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Integer size) {
		log.info("[taggedFeeds] me={} userNo={} limit={} size={}", principal.getUserNo(), userNo, limit, size);
		neighborService.assertCanViewUltary(principal, userNo);
		Integer pageSize = size != null ? size : limit;
		return ApiResponse.ok(
				myUltaryService.getTaggedFeedsOf(principal, userNo, pageSize),
				"유저 태그 게시글 조회 성공");
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

	/** 내가 차단한 사용자. 해제는 DELETE /users/{userNo}/block */
	@GetMapping("/blocks")
	public ApiResponse<List<BlockedUserItemResponse>> blocks(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[blocks] me={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(neighborService.listBlocked(principal, limit), "차단한 사용자 조회 성공");
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
