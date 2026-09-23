package me._hanho.ultary.domain.test;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.story.StoryMapper;
import me._hanho.ultary.domain.test.dto.request.PasswordEncodeRequest;
import me._hanho.ultary.domain.test.dto.response.PasswordEncodeResponse;
import me._hanho.ultary.domain.test.dto.response.StoryViewResetResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/**
 * 로컬 전용 테스트 API ({@code spring.profiles.active=local} 일 때만 빈 등록).
 * prod 프로필에서는 컨트롤러 자체가 없어 404.
 */
@Slf4j
@Profile("local")
@Validated
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

	private final PasswordEncoder passwordEncoder;
	private final StoryMapper storyMapper;

	@PostMapping("/password/encode")
	public ApiResponse<PasswordEncodeResponse> encodePassword(
			@Valid @RequestBody PasswordEncodeRequest request) {
		log.info("[encodePassword]");
		String encoded = passwordEncoder.encode(request.getPassword());
		return ApiResponse.ok(PasswordEncodeResponse.builder()
				.encodedPassword(encoded)
				.build(), "비밀번호 인코딩 성공");
	}

	/**
	 * 현재 로그인 유저의 스토리 읽음({@code ultary_story_view}) 전부 삭제.
	 * FE: development에서만 버튼 노출 → Bearer 필수.
	 */
	@DeleteMapping("/story-views")
	public ApiResponse<StoryViewResetResponse> resetStoryViews(
			@AuthenticationPrincipal UserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		int deleted = storyMapper.deleteViewsByViewerUserNo(principal.getUserNo());
		log.info("[resetStoryViews] userNo={} deleted={}", principal.getUserNo(), deleted);
		return ApiResponse.ok(StoryViewResetResponse.builder()
				.viewerUserNo(principal.getUserNo())
				.deletedCount(deleted)
				.build(), "스토리 읽음 초기화 성공");
	}
}
