package me._hanho.ultary.domain.tag;

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
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.tag.TagService;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.tags / api-memo.md §6 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

	private final TagService tagService;

	// 태그 등록
	@PostMapping
	public ApiResponse<Void> create() {
		log.info("[create]");
		tagService.create();
		return ApiResponse.ok();
	}

	// 태그 검색
	@GetMapping("/search")
	public ApiResponse<Void> search(@RequestParam(required = false) String q) {
		log.info("[search] q={}", q);
		tagService.search(q);
		return ApiResponse.ok();
	}

	// 내용 입력 시 태그 추천
	@GetMapping("/recommend")
	public ApiResponse<Void> recommend(@RequestParam(required = false) String q) {
		log.info("[recommend] q={}", q);
		tagService.recommend(q);
		return ApiResponse.ok();
	}

	/** handle 변경 가능 여부 */
	@GetMapping("/{tagId}/handle/change-availability")
	public ApiResponse<ChangeAvailabilityResponse> handleChangeAvailability(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId) {
		log.info("[handleChangeAvailability] tagId={}", tagId);
		return ApiResponse.ok(tagService.handleChangeAvailability(principal, tagId));
	}

	/** handle 변경/최초 설정 (설정·변경 후 30일 쿨다운). hashtag는 변경 불가 */
	@PatchMapping("/{tagId}/handle")
	public ApiResponse<TagResponse> changeHandle(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long tagId,
			@Valid @RequestBody ChangeTagHandleRequest request) {
		log.info("[changeHandle] tagId={} handle={}", tagId, request.getHandle());
		return ApiResponse.ok(tagService.changeHandle(principal, tagId, request));
	}

	// 태그 정보 조회 (호버·클릭)
	@GetMapping("/{tagId}")
	public ApiResponse<Void> detail(@PathVariable Long tagId) {
		log.info("[detail] tagId={}", tagId);
		tagService.getDetail(tagId);
		return ApiResponse.ok();
	}
}
