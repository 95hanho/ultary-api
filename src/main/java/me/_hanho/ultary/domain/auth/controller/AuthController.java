package me._hanho.ultary.domain.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.MeResponse;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.service.AuthService;
import me._hanho.ultary.security.principal.UserPrincipal;

/**
 * 경로 원본: ultary-web springEndpoints.auth / docs/api-memo.md §1
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// 로그인
	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		log.info("[login] loginId={}", request.getLoginId());
		return ApiResponse.ok(authService.login(request, httpRequest));
	}

	// 로그인 토큰 재발급
	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		log.info("[refresh]");
		return ApiResponse.ok(authService.refresh(request, httpRequest));
	}

	// 로그아웃
	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[logout]");
		authService.logout(principal);
		return ApiResponse.ok();
	}

	// 내 회원정보 조회
	@GetMapping("/me")
	public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[me]");
		return ApiResponse.ok(authService.me(principal));
	}

	// 회원정보 변경
	@PatchMapping("/me")
	public ApiResponse<Void> updateMe(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[updateMe]");
		authService.updateMe(principal);
		return ApiResponse.ok();
	}

	// 회원탈퇴
	@DeleteMapping("/me")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[withdraw]");
		authService.withdraw(principal);
		return ApiResponse.ok();
	}

	// 회원가입
	@PostMapping("/signup")
	public ApiResponse<Void> signup() {
		log.info("[signup]");
		authService.signup();
		return ApiResponse.ok();
	}

	// 아이디 중복확인
	@GetMapping("/login-id/check")
	public ApiResponse<Void> checkLoginId(@RequestParam String loginId) {
		log.info("[checkLoginId] loginId={}", loginId);
		authService.checkLoginId(loginId);
		return ApiResponse.ok();
	}

	// 휴대폰 인증
	@PostMapping("/phone")
	public ApiResponse<Void> phone() {
		log.info("[phone]");
		authService.requestPhoneAuth();
		return ApiResponse.ok();
	}

	// 휴대폰 인증 확인
	@PostMapping("/phone/verify")
	public ApiResponse<Void> phoneVerify() {
		log.info("[phoneVerify]");
		authService.verifyPhoneAuth();
		return ApiResponse.ok();
	}

	// 비밀번호 변경 토큰 생성
	@PostMapping("/password/token")
	public ApiResponse<Void> passwordToken() {
		log.info("[passwordToken]");
		authService.createPasswordToken();
		return ApiResponse.ok();
	}

	// 비밀번호 변경
	@PutMapping("/password")
	public ApiResponse<Void> password() {
		log.info("[password]");
		authService.changePassword();
		return ApiResponse.ok();
	}

	// 구글 소셜 로그인 시작
	@GetMapping("/social/google")
	public ApiResponse<Void> google() {
		log.info("[google]");
		authService.startGoogleLogin();
		return ApiResponse.ok();
	}

	// 구글 소셜 콜백
	@GetMapping("/social/google/callback")
	public ApiResponse<Void> googleCallback() {
		log.info("[googleCallback]");
		authService.googleCallback();
		return ApiResponse.ok();
	}

	// 카카오 소셜 로그인 시작
	@GetMapping("/social/kakao")
	public ApiResponse<Void> kakao() {
		log.info("[kakao]");
		authService.startKakaoLogin();
		return ApiResponse.ok();
	}

	// 카카오 소셜 콜백
	@GetMapping("/social/kakao/callback")
	public ApiResponse<Void> kakaoCallback() {
		log.info("[kakaoCallback]");
		authService.kakaoCallback();
		return ApiResponse.ok();
	}

	// 소셜 계정 연동
	@PostMapping("/social/link")
	public ApiResponse<Void> socialLink(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[socialLink]");
		authService.linkSocial(principal);
		return ApiResponse.ok();
	}

	// 소셜 계정 연동 해제
	@DeleteMapping("/social/unlink")
	public ApiResponse<Void> socialUnlink(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(required = false) String provider) {
		log.info("[socialUnlink] provider={}", provider);
		authService.unlinkSocial(principal, provider);
		return ApiResponse.ok();
	}
}
