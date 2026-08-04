package me._hanho.ultary.domain.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.MeResponse;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.service.AuthService;
import me._hanho.ultary.security.principal.UserPrincipal;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		return ApiResponse.ok(authService.login(request, httpRequest));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		return ApiResponse.ok(authService.refresh(request, httpRequest));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
		authService.logout(principal);
		return ApiResponse.ok();
	}

	@GetMapping("/me")
	public ApiResponse<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.ok(authService.me(principal));
	}
}
