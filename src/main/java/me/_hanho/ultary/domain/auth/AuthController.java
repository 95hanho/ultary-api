package me._hanho.ultary.domain.auth;

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
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.common.validation.ValidNickname;
import me._hanho.ultary.domain.auth.dto.request.ChangeMyPasswordRequest;
import me._hanho.ultary.domain.auth.dto.request.ChangeNicknameRequest;
import me._hanho.ultary.domain.auth.dto.request.ChangePasswordRequest;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.PasswordTokenRequest;
import me._hanho.ultary.domain.auth.dto.request.PhoneAuthRequest;
import me._hanho.ultary.domain.auth.dto.request.PhoneVerifyRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.request.SignupRequest;
import me._hanho.ultary.domain.auth.dto.request.SocialLoginRequest;
import me._hanho.ultary.domain.auth.dto.request.UpdateMeRequest;
import me._hanho.ultary.domain.auth.dto.response.AvailabilityCheckResponse;
import me._hanho.ultary.domain.auth.dto.response.MeResponse;
import me._hanho.ultary.domain.auth.dto.response.PasswordTokenResponse;
import me._hanho.ultary.domain.auth.dto.response.PhoneAuthResponse;
import me._hanho.ultary.domain.auth.dto.response.PhoneVerifyResponse;
import me._hanho.ultary.domain.auth.dto.response.SocialLoginResponse;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/**
 * 경로 원본: ultary-web springEndpoints.auth / docs/api-memo.md §1
 * 소셜 OAuth(시작/콜백)는 FE/BFF 담당. BE는 DB 조회·가입·연동만 처리.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/social/login")
	public ApiResponse<SocialLoginResponse> socialLogin(
			@Valid @RequestBody SocialLoginRequest request,
			HttpServletRequest httpRequest) {
		log.info("[socialLogin] provider={}", request.getProvider());
		return ApiResponse.ok(authService.socialLogin(request, httpRequest), "소셜 로그인 성공");
	}

	@PostMapping("/social/link")
	public ApiResponse<Void> socialLink(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody SocialLoginRequest request) {
		log.info("[socialLink] provider={}", request.getProvider());
		authService.linkSocial(principal, request);
		return ApiResponse.okEmpty("소셜 계정 연동 성공");
	}

	@DeleteMapping("/social/unlink")
	public ApiResponse<Void> socialUnlink(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam String provider) {
		log.info("[socialUnlink] provider={}", provider);
		authService.unlinkSocial(principal, provider);
		return ApiResponse.okEmpty("소셜 계정 연동 해제 성공");
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		log.info("[login] emailPresent={} phonePresent={}",
				request.getEmail() != null && !request.getEmail().isBlank(),
				request.getPhone() != null && !request.getPhone().isBlank());
		return ApiResponse.ok(authService.login(request, httpRequest), "로그인 성공");
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		log.info("[refresh]");
		return ApiResponse.ok(authService.refresh(request, httpRequest), "토큰 재발급 성공");
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[logout]");
		authService.logout(principal);
		return ApiResponse.okEmpty("로그아웃 성공");
	}

	@GetMapping("/me")
	public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[me]");
		return ApiResponse.ok(authService.me(principal), "회원정보 조회 성공");
	}

	@PatchMapping("/me")
	public ApiResponse<MeResponse> updateMe(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateMeRequest request) {
		log.info("[updateMe]");
		return ApiResponse.ok(authService.updateMe(principal, request), "회원정보 수정 성공");
	}

	@GetMapping("/me/nickname/change-availability")
	public ApiResponse<ChangeAvailabilityResponse> nicknameChangeAvailability(
			@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[nicknameChangeAvailability]");
		return ApiResponse.ok(authService.nicknameChangeAvailability(principal), "닉네임 변경 가능 여부 조회 성공");
	}

	@PatchMapping("/me/nickname")
	public ApiResponse<MeResponse> changeNickname(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangeNicknameRequest request) {
		log.info("[changeNickname] nickname={}", request.getNickname());
		return ApiResponse.ok(authService.changeNickname(principal, request), "닉네임 변경 성공");
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[withdraw]");
		authService.withdraw(principal);
		return ApiResponse.okEmpty("회원탈퇴 성공");
	}

	@PostMapping("/signup")
	public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
		log.info("[signup]");
		authService.signup(request);
		return ApiResponse.okEmpty("회원가입 성공");
	}

	@GetMapping("/nickname/check")
	public ApiResponse<AvailabilityCheckResponse> checkNickname(
			@RequestParam
			@NotBlank(message = "닉네임은 필수입니다.")
			@ValidNickname
			String nickname) {
		log.info("[checkNickname] nickname={}", nickname);
		return ApiResponse.ok(authService.checkNickname(nickname), "닉네임 중복확인 성공");
	}

	@PostMapping("/phone")
	public ApiResponse<PhoneAuthResponse> phone(@Valid @RequestBody PhoneAuthRequest request) {
		log.info("[phone] phone={}", request.getPhone());
		return ApiResponse.ok(authService.requestPhoneAuth(request), "휴대폰 인증 요청 성공");
	}

	@PostMapping("/phone/verify")
	public ApiResponse<PhoneVerifyResponse> phoneVerify(@Valid @RequestBody PhoneVerifyRequest request) {
		log.info("[phoneVerify]");
		return ApiResponse.ok(authService.verifyPhoneAuth(request), "휴대폰 인증 확인 성공");
	}

	@PostMapping("/password/token")
	public ApiResponse<PasswordTokenResponse> passwordToken(@Valid @RequestBody PasswordTokenRequest request) {
		log.info("[passwordToken] phone={}", request.getPhone());
		return ApiResponse.ok(authService.createPasswordToken(request), "비밀번호 변경 토큰 발급 성공");
	}

	@PutMapping("/password")
	public ApiResponse<Void> password(@Valid @RequestBody ChangePasswordRequest request) {
		log.info("[password]");
		authService.changePassword(request);
		return ApiResponse.okEmpty("비밀번호 변경 성공");
	}

	@PutMapping("/password/me")
	public ApiResponse<Void> changeMyPassword(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangeMyPasswordRequest request) {
		log.info("[changeMyPassword]");
		authService.changeMyPassword(principal, request);
		return ApiResponse.okEmpty("비밀번호 변경 성공");
	}
}
