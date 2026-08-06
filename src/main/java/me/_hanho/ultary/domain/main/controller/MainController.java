package me._hanho.ultary.domain.main.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.main.service.MainService;

/** 경로 원본: springEndpoints.main / api-memo.md §2 */
@Slf4j
@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController {

	private final MainService mainService;

	// 주민 스토리 있는 목록 조회
	@GetMapping("/stories/owners")
	public ApiResponse<Void> storyOwners() {
		log.info("[storyOwners]");
		mainService.getStoryOwners();
		return ApiResponse.ok();
	}

	// 주민 스토리 조회
	@GetMapping("/stories")
	public ApiResponse<Void> stories(@RequestParam(required = false) Long userNo) {
		log.info("[stories] userNo={}", userNo);
		mainService.getStories(userNo);
		return ApiResponse.ok();
	}

	// 주민 게시글 조회 (무한 스크롤)
	@GetMapping("/feeds")
	public ApiResponse<Void> feeds() {
		log.info("[feeds]");
		mainService.getFeeds();
		return ApiResponse.ok();
	}

	// 검색 (유저 / 반려동물 / 태그 / 게시글)
	@GetMapping("/search")
	public ApiResponse<Void> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String type) {
		log.info("[search] q={}, type={}", q, type);
		mainService.search(q, type);
		return ApiResponse.ok();
	}
}
