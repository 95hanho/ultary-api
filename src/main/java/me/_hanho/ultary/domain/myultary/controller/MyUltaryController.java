package me._hanho.ultary.domain.myultary.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.myultary.service.MyUltaryService;

/** 경로 원본: springEndpoints.myUltary / api-memo.md §3 */
@Slf4j
@RestController
@RequestMapping("/api/v1/my-ultary")
@RequiredArgsConstructor
public class MyUltaryController {

	private final MyUltaryService myUltaryService;

	// 마이울타리 정보 조회 (프로필·스토리유무·주민수·이웃수·상태글)
	@GetMapping
	public ApiResponse<Void> profile() {
		log.info("[profile]");
		myUltaryService.getProfile();
		return ApiResponse.ok();
	}

	// MY 게시글 조회 (그리드)
	@GetMapping("/feeds")
	public ApiResponse<Void> feeds() {
		log.info("[feeds]");
		myUltaryService.getFeeds();
		return ApiResponse.ok();
	}

	// MY 게시글 상세 (피드형)
	@GetMapping("/feeds/{feedId}")
	public ApiResponse<Void> feedDetail(@PathVariable Long feedId) {
		log.info("[feedDetail] feedId={}", feedId);
		myUltaryService.getFeedDetail(feedId);
		return ApiResponse.ok();
	}

	// 저장한 게시글 조회
	@GetMapping("/saved-feeds")
	public ApiResponse<Void> savedFeeds() {
		log.info("[savedFeeds]");
		myUltaryService.getSavedFeeds();
		return ApiResponse.ok();
	}

	// 자신이 태그된 게시글 조회
	@GetMapping("/tagged-feeds")
	public ApiResponse<Void> taggedFeeds() {
		log.info("[taggedFeeds]");
		myUltaryService.getTaggedFeeds();
		return ApiResponse.ok();
	}

	// 프로필 사진 변경
	@PatchMapping("/profile-image")
	public ApiResponse<Void> profileImage() {
		log.info("[profileImage]");
		myUltaryService.updateProfileImage();
		return ApiResponse.ok();
	}

	// 소개글 변경
	@PatchMapping("/bio")
	public ApiResponse<Void> bio() {
		log.info("[bio]");
		myUltaryService.updateBio();
		return ApiResponse.ok();
	}

	// 스토리 등록
	@PostMapping("/stories")
	public ApiResponse<Void> createStory() {
		log.info("[createStory]");
		myUltaryService.createStory();
		return ApiResponse.ok();
	}

	// 스토리 삭제
	@DeleteMapping("/stories/{storyId}")
	public ApiResponse<Void> deleteStory(@PathVariable Long storyId) {
		log.info("[deleteStory] storyId={}", storyId);
		myUltaryService.deleteStory(storyId);
		return ApiResponse.ok();
	}
}
