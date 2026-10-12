package me._hanho.ultary.domain.admin;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.admin.dto.request.AdminLoginRequest;
import me._hanho.ultary.domain.admin.model.Admin;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.model.Token;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.jwt.TokenOwnerType;
import me._hanho.ultary.security.principal.AdminPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private final AdminMapper adminMapper;
	private final TokenMapper tokenMapper;
	private final JwtTokenProvider jwtTokenProvider;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public TokenResponse login(AdminLoginRequest request, HttpServletRequest httpRequest) {
		Admin admin = adminMapper.findByLoginId(request.getLoginId().trim());
		if (admin != null && "SUSPENDED".equals(admin.getStatus())) {
			throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
		}
		if (admin == null
				|| !"ACTIVE".equals(admin.getStatus())
				|| !StringUtils.hasText(admin.getPassword())
				|| !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
			throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
		}

		adminMapper.updateLastLoginAt(admin.getAdminNo());
		log.info("[adminLogin] adminNo={}", admin.getAdminNo());
		return issueTokens(admin, httpRequest);
	}

	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
		Claims claims = jwtTokenProvider.parseRefreshToken(request.getRefreshToken());
		if (jwtTokenProvider.getOwnerType(claims) != TokenOwnerType.ADMIN) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		Long adminNo = jwtTokenProvider.getSubjectAsLong(claims);
		Token storedToken = tokenMapper.findValidAdminRefreshToken(adminNo, request.getRefreshToken());
		if (storedToken == null) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		Admin admin = adminMapper.findByAdminNo(adminNo);
		if (admin != null && "SUSPENDED".equals(admin.getStatus())) {
			throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
		}
		if (admin == null || !"ACTIVE".equals(admin.getStatus())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		tokenMapper.revokeByTokenId(storedToken.getTokenId());
		log.info("[adminRefresh] adminNo={}", adminNo);
		return issueTokens(admin, httpRequest);
	}

	@Transactional
	public void logout(AdminPrincipal principal) {
		if (principal == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		tokenMapper.revokeAllByAdminNo(principal.getAdminNo());
		log.info("[adminLogout] adminNo={}", principal.getAdminNo());
	}

	private TokenResponse issueTokens(Admin admin, HttpServletRequest httpRequest) {
		String accessToken = jwtTokenProvider.createAdminAccessToken(admin.getAdminNo());
		String refreshToken = jwtTokenProvider.createRefreshToken(admin.getAdminNo(), TokenOwnerType.ADMIN);

		Token token = new Token();
		token.setOwnerType(TokenOwnerType.ADMIN.name());
		token.setAdminNo(admin.getAdminNo());
		token.setConnectIp(resolveClientIp(httpRequest));
		token.setConnectAgent(trimAgent(httpRequest.getHeader("User-Agent")));
		token.setRefreshToken(refreshToken);
		token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.refreshLifetimeSeconds()));
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
