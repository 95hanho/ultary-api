package me._hanho.ultary.domain.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.exception.NotImplemented;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.MeResponse;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.mapper.TokenMapper;
import me._hanho.ultary.domain.auth.model.Token;
import me._hanho.ultary.domain.user.mapper.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.jwt.TokenOwnerType;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final TokenMapper tokenMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
		User user = userMapper.findByLoginId(request.getLoginId());
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		if (!"ACTIVE".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		}

		return issueTokens(user, httpRequest);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
		Claims claims = jwtTokenProvider.parseRefreshToken(request.getRefreshToken());
		if (jwtTokenProvider.getOwnerType(claims) != TokenOwnerType.USER) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		Long userNo = jwtTokenProvider.getSubjectAsLong(claims);
		Token storedToken = tokenMapper.findValidUserRefreshToken(userNo, request.getRefreshToken());
		if (storedToken == null) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		}

		tokenMapper.revokeByTokenId(storedToken.getTokenId());
		return issueTokens(user, httpRequest);
	}

	@Transactional
	public void logout(UserPrincipal principal) {
		tokenMapper.revokeAllByUserNo(principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public MeResponse me(UserPrincipal principal) {
		User user = userMapper.findActiveByUserNo(principal.getUserNo());
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return MeResponse.builder()
				.userNo(user.getUserNo())
				.loginId(user.getLoginId())
				.nickname(user.getNickname())
				.build();
	}

	public void updateMe(UserPrincipal principal) {
		NotImplemented.yet();
	}

	public void withdraw(UserPrincipal principal) {
		NotImplemented.yet();
	}

	public void signup() {
		NotImplemented.yet();
	}

	public void checkLoginId(String loginId) {
		NotImplemented.yet();
	}

	public void requestPhoneAuth() {
		NotImplemented.yet();
	}

	public void verifyPhoneAuth() {
		NotImplemented.yet();
	}

	public void createPasswordToken() {
		NotImplemented.yet();
	}

	public void changePassword() {
		NotImplemented.yet();
	}

	public void startGoogleLogin() {
		NotImplemented.yet();
	}

	public void googleCallback() {
		NotImplemented.yet();
	}

	public void startKakaoLogin() {
		NotImplemented.yet();
	}

	public void kakaoCallback() {
		NotImplemented.yet();
	}

	public void linkSocial(UserPrincipal principal) {
		NotImplemented.yet();
	}

	public void unlinkSocial(UserPrincipal principal, String provider) {
		NotImplemented.yet();
	}

	private TokenResponse issueTokens(User user, HttpServletRequest httpRequest) {
		String accessToken = jwtTokenProvider.createUserAccessToken(user.getUserNo());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserNo(), TokenOwnerType.USER);

		Token token = new Token();
		token.setOwnerType(TokenOwnerType.USER.name());
		token.setUserNo(user.getUserNo());
		token.setConnectIp(resolveClientIp(httpRequest));
		token.setConnectAgent(trimAgent(httpRequest.getHeader("User-Agent")));
		token.setRefreshToken(refreshToken);
		token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiresInSeconds()));
		tokenMapper.insert(token);

		return TokenResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.tokenType("Bearer")
				.expiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds())
				.build();
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String trimAgent(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
	}
}
