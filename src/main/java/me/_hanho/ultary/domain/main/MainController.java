package me._hanho.ultary.domain.main;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.main.dto.request.SavePetTagHistoryRequest;
import me._hanho.ultary.domain.main.dto.request.SaveSearchHistoryRequest;
import me._hanho.ultary.domain.main.dto.response.MainFeedPageResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchResponse;
import me._hanho.ultary.domain.main.dto.response.PetTagHistoryItemResponse;
import me._hanho.ultary.domain.main.dto.response.PetTagHistoryPageResponse;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryItemResponse;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryPageResponse;
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
	public ApiResponse<MainFeedPageResponse> feeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Long cursorFeedId,
			@RequestParam(required = false) Integer limit) {
		log.info("[feeds] userNo={} cursorFeedId={} limit={}",
				principal.getUserNo(), cursorFeedId, limit);
		return ApiResponse.ok(mainService.getFeeds(principal, cursorFeedId, limit), "주민 게시글 조회 성공");
	}

	/** 나중에 추천 알고리즘 추가해야함. 지금은 조회 가능한 전체 피드를 limit건 */
	@GetMapping("/feeds/recommended")
	public ApiResponse<List<FeedResponse>> recommendedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[recommendedFeeds] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(mainService.getRecommendedFeeds(principal, limit), "메인 추천 게시글 조회 성공");
	}

	@GetMapping("/search")
	public ApiResponse<MainSearchResponse> search(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam String q,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Integer limit) {
		log.info("[search] userNo={} q={} type={}", principal.getUserNo(), q, type);
		return ApiResponse.ok(mainService.search(principal, q, type, limit), "검색 성공");
	}

	/** 태그명 검색 결과에서 고른 태그의 게시글 그리드 */
	@GetMapping("/search/tags/{tagId}/feeds")
	public ApiResponse<List<FeedGridItemResponse>> searchTagFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId,
			@RequestParam(required = false) Integer limit) {
		log.info("[searchTagFeeds] userNo={} tagId={} limit={}", principal.getUserNo(), tagId, limit);
		return ApiResponse.ok(
				mainService.listFeedsByTag(principal, tagId, limit),
				"태그 게시글 조회 성공");
	}

	/** 나중에 추천 알고리즘 추가해야함. 지금은 조회 가능한 전체 피드를 limit건 */
	@GetMapping("/search/recommended")
	public ApiResponse<List<FeedResponse>> searchRecommendedFeeds(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) Integer limit) {
		log.info("[searchRecommendedFeeds] userNo={} limit={}", principal.getUserNo(), limit);
		return ApiResponse.ok(
				mainService.getSearchRecommendedFeeds(principal, limit),
				"검색 추천 게시글 조회 성공");
	}

	/** 스토리 @·사진 태그 모달의 최근 펫 20건. 검색 최근 울타리와 별도 */
	@GetMapping("/pet-tags/recent")
	public ApiResponse<PetTagHistoryPageResponse> recentPetTags(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[recentPetTags] userNo={}", principal.getUserNo());
		return ApiResponse.ok(mainService.getRecentPetTags(principal), "최근 펫 태그 조회 성공");
	}

	/** 멘션으로 펫을 고를 때 저장. 같은 펫은 used_at만 갱신 */
	@PostMapping("/pet-tags/recent")
	public ApiResponse<PetTagHistoryItemResponse> saveRecentPetTag(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody SavePetTagHistoryRequest request) {
		log.info("[saveRecentPetTag] userNo={} petId={}", principal.getUserNo(), request.getPetId());
		return ApiResponse.ok(
				mainService.saveRecentPetTag(principal, request.getPetId()),
				"최근 펫 태그 저장 성공");
	}

	/** 내 최근 펫 태그 전부 삭제 */
	@DeleteMapping("/pet-tags/recent")
	public ApiResponse<Void> clearRecentPetTags(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[clearRecentPetTags] userNo={}", principal.getUserNo());
		mainService.clearRecentPetTags(principal);
		return ApiResponse.okEmpty("최근 펫 태그 모두 삭제 성공");
	}

	/** 검색창을 열면 최근 들어간 울타리 5건 */
	@GetMapping("/search/recent")
	public ApiResponse<SearchHistoryPageResponse> recentSearches(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[recentSearches] userNo={}", principal.getUserNo());
		return ApiResponse.ok(mainService.getRecentSearches(principal), "최근 검색 조회 성공");
	}

	/** 최근 검색 더보기 20건. cursorHistoryId는 직전 응답의 nextCursorHistoryId */
	@GetMapping("/search/recent/more")
	public ApiResponse<SearchHistoryPageResponse> moreRecentSearches(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam Long cursorHistoryId) {
		log.info("[moreRecentSearches] userNo={} cursorHistoryId={}",
				principal.getUserNo(), cursorHistoryId);
		return ApiResponse.ok(
				mainService.getMoreRecentSearches(principal, cursorHistoryId),
				"최근 검색 더보기 성공");
	}

	/** 검색 후 그 유저 울타리에 들어갈 때 저장 */
	@PostMapping("/search/recent")
	public ApiResponse<SearchHistoryItemResponse> saveRecentSearch(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody SaveSearchHistoryRequest request) {
		log.info("[saveRecentSearch] userNo={} targetUserNo={}",
				principal.getUserNo(), request.getTargetUserNo());
		return ApiResponse.ok(
				mainService.saveRecentSearch(principal, request.getTargetUserNo()),
				"최근 검색 저장 성공");
	}

	/** 최근 검색 한 건 삭제. userSearchHistoryId는 목록 항목 값 */
	@DeleteMapping("/search/recent/{userSearchHistoryId}")
	public ApiResponse<Void> deleteRecentSearch(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long userSearchHistoryId) {
		log.info("[deleteRecentSearch] userNo={} userSearchHistoryId={}",
				principal.getUserNo(), userSearchHistoryId);
		mainService.deleteRecentSearch(principal, userSearchHistoryId);
		return ApiResponse.okEmpty("최근 검색 삭제 성공");
	}

	/** 최근 검색 모두 지우기 */
	@DeleteMapping("/search/recent")
	public ApiResponse<Void> clearRecentSearches(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[clearRecentSearches] userNo={}", principal.getUserNo());
		mainService.clearRecentSearches(principal);
		return ApiResponse.okEmpty("최근 검색 모두 삭제 성공");
	}
}
