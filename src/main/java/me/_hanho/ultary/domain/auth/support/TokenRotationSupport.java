package me._hanho.ultary.domain.auth.support;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.model.Token;
import me._hanho.ultary.security.jwt.JwtTokenProvider;

/** 계정 행 잠금은 Spring 인스턴스가 달라도 refresh/logout을 같은 순서로 처리한다. */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class TokenRotationSupport {

	private final TokenMapper tokens;
	private final JwtTokenProvider jwt;

	public Token lockUserToken(Long userNo, String refreshToken) {
		if (tokens.lockUserForRefresh(userNo) == null) throw invalid();
		return tokens.findUserTokenForUpdate(userNo, refreshToken);
	}

	public Token lockAdminToken(Long adminNo, String refreshToken) {
		if (tokens.lockAdminForRefresh(adminNo) == null) throw invalid();
		return tokens.findAdminTokenForUpdate(adminNo, refreshToken);
	}

	/** 최초로 받은 이전 토큰의 20초 창 안에서만 활성 후속 토큰을 찾는다. */
	public Token activeSuccessor(Token original) {
		if (original == null || !Boolean.TRUE.equals(original.getIsRevoked())
				|| !Boolean.TRUE.equals(original.getRotationRetryAllowed())) throw invalid();
		Token current = original;
		for (int depth = 0; depth < 8; depth++) {
			if (current.getReplacedByTokenId() == null) throw invalid();
			current = tokens.findTokenByIdForUpdate(current.getReplacedByTokenId());
			if (current == null || !Objects.equals(original.getOwnerType(), current.getOwnerType())
					|| !Objects.equals(original.getUserNo(), current.getUserNo())
					|| !Objects.equals(original.getAdminNo(), current.getAdminNo())
					|| (StringUtils.hasText(original.getSessionId())
							&& !Objects.equals(original.getSessionId(), current.getSessionId()))) throw invalid();
			if (!Boolean.TRUE.equals(current.getIsRevoked())) return current;
		}
		throw invalid();
	}

	public TokenResponse replay(Token successor) {
		String access = "ADMIN".equals(successor.getOwnerType())
				? jwt.createAdminAccessToken(successor.getAdminNo()) : jwt.createUserAccessToken(successor.getUserNo());
		return TokenResponse.builder().accessToken(access).refreshToken(successor.getRefreshToken())
				.tokenType("Bearer").expiresIn(jwt.getAccessTokenExpiresInSeconds()).build();
	}

	public Token successorForLogout(Token original) {
		try {
			return activeSuccessor(original);
		} catch (BusinessException invalidToken) {
			return null; // 회전된 세션도 이미 로그아웃되었다면 멱등 성공 처리한다.
		}
	}

	public void complete(Token previous, Token replacement) {
		if (tokens.completeRotation(previous.getTokenId(), replacement.getTokenId()) != 1) throw invalid();
	}

	private BusinessException invalid() { return new BusinessException(ErrorCode.INVALID_TOKEN); }
}
