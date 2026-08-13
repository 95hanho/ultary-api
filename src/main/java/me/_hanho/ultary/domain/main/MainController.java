package me._hanho.ultary.domain.main;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;

/** 경로 원본: springEndpoints.main / api-memo.md §2 */
@Slf4j
@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController {

	private final MainService mainService;

	@GetMapping("/stories/owners")
	public ApiResponse<Void> storyOwners() {
		log.info("[storyOwners]");
		mainService.getStoryOwners();
		return ApiResponse.okEmpty("스토리 있는 주민 목록 조회 성공");
	}

	@GetMapping("/stories")
	public ApiResponse<Void> stories(@RequestParam(required = false) Long userNo) {
		log.info("[stories] userNo={}", userNo);
		mainService.getStories(userNo);
		return ApiResponse.okEmpty("주민 스토리 조회 성공");
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
