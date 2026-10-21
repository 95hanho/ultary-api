package me._hanho.ultary.domain.admin;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.request.AdminLoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;

/**
 * 관리자 인증은 회원 토큰과 비밀키가 다르다.
 * 같은 브라우저라도 쿠키를 따로 두면 회원 로그인과 겹치지 않는다.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AdminAuthService adminAuthService;

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(
			@Valid @RequestBody AdminLoginRequest request,
			HttpServletRequest httpRequest) {
		log.info("[adminLogin] loginId={}", request.getLoginId());
		return ApiResponse.ok(adminAuthService.login(request, httpRequest), "관리자 로그인 성공");
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		log.info("[adminRefresh]");
		return ApiResponse.ok(adminAuthService.refresh(request, httpRequest), "관리자 토큰 재발급 성공");
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		log.info("[adminLogout]");
		adminAuthService.logout(request);
		return ApiResponse.okEmpty("관리자 로그아웃 성공");
	}
}
