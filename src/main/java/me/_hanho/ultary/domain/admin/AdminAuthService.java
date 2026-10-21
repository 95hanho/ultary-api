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
import me._hanho.ultary.domain.auth.support.TokenSessionSupport;
import me._hanho.ultary.domain.auth.support.TokenRotationSupport;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.jwt.TokenOwnerType;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

	private final AdminMapper adminMapper;
	private final TokenMapper tokenMapper;
	private final TokenRotationSupport tokenRotation;
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
		Token storedToken = tokenRotation.lockAdminToken(adminNo, request.getRefreshToken());
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

		if (Boolean.TRUE.equals(storedToken.getIsRevoked())) {
			return tokenRotation.replay(tokenRotation.activeSuccessor(storedToken));
		}
		log.info("[adminRefresh] adminNo={}", adminNo);
		return issueTokens(admin, httpRequest, storedToken);
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		Claims claims = jwtTokenProvider.parseRefreshToken(request.getRefreshToken());
		if (jwtTokenProvider.getOwnerType(claims) != TokenOwnerType.ADMIN) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		Long adminNo = jwtTokenProvider.getSubjectAsLong(claims);
		Token token = tokenRotation.lockAdminToken(adminNo, request.getRefreshToken());
		if (token == null) return;
		if (Boolean.TRUE.equals(token.getIsRevoked())) {
			if (token.getReplacedByTokenId() == null || !Boolean.TRUE.equals(token.getRotationRetryAllowed())) return;
			token = tokenRotation.successorForLogout(token);
			if (token == null) return;
		}
		if (StringUtils.hasText(token.getSessionId())) {
			tokenMapper.revokeAdminSession(adminNo, token.getSessionId());
		} else {
			tokenMapper.revokeByTokenId(token.getTokenId());
		}
		log.info("[adminLogout] adminNo={}", adminNo);
	}

	private TokenResponse issueTokens(Admin admin, HttpServletRequest httpRequest) {
		return issueTokens(admin, httpRequest, null);
	}

	private TokenResponse issueTokens(Admin admin, HttpServletRequest httpRequest, Token previous) {
		Token token = TokenSessionSupport.newToken(previous, httpRequest);
		String accessToken = jwtTokenProvider.createAdminAccessToken(admin.getAdminNo());
		String refreshToken = jwtTokenProvider.createRefreshToken(admin.getAdminNo(), TokenOwnerType.ADMIN, token.getSessionId());

		token.setOwnerType(TokenOwnerType.ADMIN.name());
		token.setAdminNo(admin.getAdminNo());
		token.setRefreshToken(refreshToken);
		token.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.refreshLifetimeSeconds()));
		tokenMapper.insert(token);
		if (previous != null) tokenRotation.complete(previous, token);

		return TokenResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.tokenType("Bearer")
				.expiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds())
				.build();
	}

}
