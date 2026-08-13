package me._hanho.ultary.domain.tag;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.domain.tag.dto.request.ChangeTagHandleRequest;
import me._hanho.ultary.domain.tag.dto.request.CreateTagRequest;
import me._hanho.ultary.domain.tag.dto.request.UpdateTagRequest;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.tags / api-memo.md §6 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

	private final TagService tagService;

	@PostMapping
	public ApiResponse<TagResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateTagRequest request) {
		log.info("[create] hashtag={} handle={}", request.getHashtag(), request.getHandle());
		return ApiResponse.ok(tagService.create(principal, request), "태그 등록 성공");
	}

	@GetMapping("/search")
	public ApiResponse<List<TagResponse>> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Integer limit) {
		log.info("[search] q={} limit={}", q, limit);
		return ApiResponse.ok(tagService.search(q, limit), "태그 검색 성공");
	}

	@GetMapping("/recommend")
	public ApiResponse<List<TagResponse>> recommend(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Integer limit) {
		log.info("[recommend] q={} limit={}", q, limit);
		return ApiResponse.ok(tagService.recommend(q, limit), "태그 추천 조회 성공");
	}

	@GetMapping("/{tagId}/handle/change-availability")
	public ApiResponse<ChangeAvailabilityResponse> handleChangeAvailability(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId) {
		log.info("[handleChangeAvailability] tagId={}", tagId);
		return ApiResponse.ok(tagService.handleChangeAvailability(principal, tagId), "핸들 변경 가능 여부 조회 성공");
	}

	@PatchMapping("/{tagId}/handle")
	public ApiResponse<TagResponse> changeHandle(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId,
			@Valid @RequestBody ChangeTagHandleRequest request) {
		log.info("[changeHandle] tagId={} handle={}", tagId, request.getHandle());
		return ApiResponse.ok(tagService.changeHandle(principal, tagId, request), "핸들 변경 성공");
	}

	@PatchMapping("/{tagId}")
	public ApiResponse<TagResponse> update(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId,
			@Valid @RequestBody UpdateTagRequest request) {
		log.info("[update] tagId={}", tagId);
		return ApiResponse.ok(tagService.update(principal, tagId, request), "태그 수정 성공");
	}

	@GetMapping("/{tagId}")
	public ApiResponse<TagResponse> detail(@PathVariable Long tagId) {
		log.info("[detail] tagId={}", tagId);
		return ApiResponse.ok(tagService.getDetail(tagId), "태그 조회 성공");
	}
}
