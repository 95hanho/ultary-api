package me._hanho.ultary.domain.main;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.main / api-memo.md §2 */
@Slf4j
@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController {

	private final MainService mainService;

	/** 주민(팔로잉) 중 활성 스토리 있는 목록 */
	@GetMapping("/stories/owners")
	public ApiResponse<List<StoryOwnerResponse>> storyOwners(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[storyOwners] userNo={}", principal.getUserNo());
		return ApiResponse.ok(mainService.getStoryOwners(principal), "스토리 있는 주민 목록 조회 성공");
	}

	/** 특정 유저 활성 스토리 목록 (본인 또는 ACCEPTED 이웃) */
	@GetMapping("/stories")
	public ApiResponse<List<StoryResponse>> stories(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam Long userNo) {
		log.info("[stories] userNo={}", userNo);
		return ApiResponse.ok(mainService.getStories(principal, userNo), "주민 스토리 조회 성공");
	}

	@GetMapping("/feeds")
	public ApiResponse<Void> feeds() {
		log.info("[feeds]");
		mainService.getFeeds();
		return ApiResponse.okEmpty("주민 게시글 조회 성공");
	}

	@GetMapping("/search")
	public ApiResponse<Void> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String type) {
		log.info("[search] q={}, type={}", q, type);
		mainService.search(q, type);
		return ApiResponse.okEmpty("검색 성공");
	}
}
