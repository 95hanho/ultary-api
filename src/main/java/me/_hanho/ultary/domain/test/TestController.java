package me._hanho.ultary.domain.test;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.story.StoryMapper;
import me._hanho.ultary.domain.test.dto.request.PasswordEncodeRequest;
import me._hanho.ultary.domain.test.dto.response.MentionIdCooldownResetResponse;
import me._hanho.ultary.domain.test.dto.response.NicknameCooldownResetResponse;
import me._hanho.ultary.domain.test.dto.response.PasswordEncodeResponse;
import me._hanho.ultary.domain.test.dto.response.StoryViewResetResponse;
import me._hanho.ultary.domain.test.dto.response.TokenResetResponse;
import me._hanho.ultary.domain.user.UserMapper;
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
	private final UserMapper userMapper;
	private final PetMapper petMapper;
	private final TokenMapper tokenMapper;

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

	/**
	 * 로그인 유저의 {@code nickname_changed_at}을 올해 1월 1일로 되돌려 7일 쿨다운을 푼다.
	 */
	@PostMapping("/nickname-cooldown")
	public ApiResponse<NicknameCooldownResetResponse> resetNicknameCooldown(
			@AuthenticationPrincipal UserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		LocalDateTime changedAt = LocalDateTime.of(LocalDateTime.now().getYear(), 1, 1, 0, 0);
		int updated = userMapper.updateNicknameChangedAt(principal.getUserNo(), changedAt);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		log.info("[resetNicknameCooldown] userNo={} nicknameChangedAt={}", principal.getUserNo(), changedAt);
		return ApiResponse.ok(NicknameCooldownResetResponse.builder()
				.userNo(principal.getUserNo())
				.nicknameChangedAt(changedAt)
				.build(), "닉네임 변경 기간 초기화 성공");
	}

	/**
	 * 내 펫의 {@code mention_id_changed_at}을 올해 1월 1일로 되돌려 30일 쿨다운을 푼다.
	 */
	@PostMapping("/pets/{petId}/mention-id-cooldown")
	public ApiResponse<MentionIdCooldownResetResponse> resetMentionIdCooldown(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long petId) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		LocalDateTime changedAt = LocalDateTime.of(LocalDateTime.now().getYear(), 1, 1, 0, 0);
		int updated = petMapper.updateMentionIdChangedAt(petId, principal.getUserNo(), changedAt);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[resetMentionIdCooldown] petId={} mentionIdChangedAt={}", petId, changedAt);
		return ApiResponse.ok(MentionIdCooldownResetResponse.builder()
				.petId(petId)
				.mentionIdChangedAt(changedAt)
				.build(), "멘션 ID 변경 기간 초기화 성공");
	}

	/**
	 * 로그인 유저의 리프레시 토큰을 모두 폐기한다.
	 * 액세스 토큰은 DB에 없으므로 쿠키 삭제는 호출 쪽(FE)이 한다.
	 */
	@DeleteMapping("/tokens")
	public ApiResponse<TokenResetResponse> resetTokens(
			@AuthenticationPrincipal UserPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		int revoked = tokenMapper.revokeAllByUserNo(principal.getUserNo());
		log.info("[resetTokens] userNo={} revoked={}", principal.getUserNo(), revoked);
		return ApiResponse.ok(TokenResetResponse.builder()
				.userNo(principal.getUserNo())
				.revokedCount(revoked)
				.build(), "액세스·리프레시 토큰 초기화 성공");
	}
}
