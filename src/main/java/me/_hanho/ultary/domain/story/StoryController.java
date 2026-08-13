package me._hanho.ultary.domain.story;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 스토리 공통 (읽음 처리 등) */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

	private final StoryService storyService;

	/** 스토리 읽음 처리 (본인 스토리는 기록하지 않음) */
	@PostMapping("/{storyId}/view")
	public ApiResponse<StoryResponse> markViewed(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long storyId) {
		log.info("[markViewed] storyId={}", storyId);
		return ApiResponse.ok(storyService.markViewed(principal, storyId), "스토리 읽음 처리 성공");
	}
}
