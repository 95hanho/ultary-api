package me._hanho.ultary.domain.myultary;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.myultary.dto.request.UpdateMyBioRequest;
import me._hanho.ultary.domain.myultary.dto.request.UpdateMyProfileImageRequest;
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.myultary.dto.response.MyUltaryProfileResponse;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.myUltary / api-memo.md §3 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/my-ultary")
@RequiredArgsConstructor
public class MyUltaryController {

	private final MyUltaryService myUltaryService;

	/** 마이울타리 정보 (프로필·스토리유무·주민/이웃수·펫/피드수) */
	@GetMapping
	public ApiResponse<MyUltaryProfileResponse> profile(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[profile] userNo={}", principal.getUserNo());
		return ApiResponse.ok(myUltaryService.getProfile(principal), "마이울타리 조회 성공");
	}

	/** MY 게시글 그리드 */
	@GetMapping("/feeds")
	public ApiResponse<List<FeedGridItemResponse>> feeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[feeds] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(myUltaryService.getFeeds(principal, limit), "내 게시글 목록 조회 성공");
	}

	/** MY 게시글 상세 (본인 작성분만) */
	@GetMapping("/feeds/{feedId}")
	public ApiResponse<FeedResponse> feedDetail(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[feedDetail] feedId={}", feedId);
		return ApiResponse.ok(myUltaryService.getFeedDetail(principal, feedId), "내 게시글 상세 조회 성공");
	}

	/** 저장한 게시글 그리드 */
	@GetMapping("/saved-feeds")
	public ApiResponse<List<FeedGridItemResponse>> savedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[savedFeeds] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(myUltaryService.getSavedFeeds(principal, limit), "저장한 게시글 조회 성공");
	}

	/** 내 펫이 COLLABORATOR이거나 사진 @멘션된 게시글 */
	@GetMapping("/tagged-feeds")
	public ApiResponse<List<FeedGridItemResponse>> taggedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[taggedFeeds] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(myUltaryService.getTaggedFeeds(principal, limit), "태그된 게시글 조회 성공");
	}

	/** 프로필 사진 변경 (fileId는 files 업로드 후) */
	@PatchMapping("/profile-image")
	public ApiResponse<MyUltaryProfileResponse> profileImage(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateMyProfileImageRequest request) {
		log.info("[profileImage] profileFileId={} remove={}",
				request.getProfileFileId(), request.getRemoveProfileFile());
		return ApiResponse.ok(myUltaryService.updateProfileImage(principal, request), "프로필 사진 변경 성공");
	}

	/** 소개글 변경 (Auth PATCH /me 의 bio와 동일 컬럼) */
	@PatchMapping("/bio")
	public ApiResponse<MyUltaryProfileResponse> bio(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateMyBioRequest request) {
		log.info("[bio]");
		return ApiResponse.ok(myUltaryService.updateBio(principal, request), "소개글 변경 성공");
	}

	/** 내 활성 스토리 목록 (24h 이내) */
	@GetMapping("/stories")
	public ApiResponse<List<StoryResponse>> myStories(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[myStories] userNo={}", principal.getUserNo());
		return ApiResponse.ok(myUltaryService.listMyStories(principal), "내 스토리 목록 조회 성공");
	}

	/** 스토리 등록 (사진/짧은 영상, 24시간) */
	@PostMapping("/stories")
	public ApiResponse<StoryResponse> createStory(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateStoryRequest request) {
		log.info("[createStory] fileId={} mediaType={}", request.getFileId(), request.getMediaType());
		return ApiResponse.ok(myUltaryService.createStory(principal, request), "스토리 등록 성공");
	}

	/** 스토리 삭제 (본인만) */
	@DeleteMapping("/stories/{storyId}")
	public ApiResponse<Void> deleteStory(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long storyId) {
		log.info("[deleteStory] storyId={}", storyId);
		myUltaryService.deleteStory(principal, storyId);
		return ApiResponse.okEmpty("스토리 삭제 성공");
	}
}
