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
import me._hanho.ultary.domain.auth.TokenMapper;

import me._hanho.ultary.domain.test.dto.request.PasswordEncodeRequest;

import me._hanho.ultary.domain.test.dto.response.PasswordEncodeResponse;

import me._hanho.ultary.domain.test.dto.response.TokenResetResponse;

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
