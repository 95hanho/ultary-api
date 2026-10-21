package me._hanho.ultary.domain.auth.support;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import me._hanho.ultary.domain.auth.model.Token;

/** 회전한 토큰에서도 최초 로그인 정보와 세션 식별자를 보존한다. */
public final class TokenSessionSupport {

	private TokenSessionSupport() {}

	public static Token newToken(Token previous, HttpServletRequest request) {
		Token token = new Token();
		token.setSessionId(previous != null && StringUtils.hasText(previous.getSessionId())
				? previous.getSessionId() : UUID.randomUUID().toString());
		token.setLoggedInAt(previous == null ? LocalDateTime.now()
				: previous.getLoggedInAt() != null ? previous.getLoggedInAt()
				: previous.getCreatedAt() != null ? previous.getCreatedAt() : LocalDateTime.now());
		if (previous != null) {
			token.setConnectIp(previous.getConnectIp());
			token.setConnectAgent(previous.getConnectAgent());
		} else {
			String forwarded = request.getHeader("X-Forwarded-For");
			String ip = StringUtils.hasText(forwarded) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
			token.setConnectIp(trim(ip, 50));
			token.setConnectAgent(trim(request.getHeader("User-Agent"), 200));
		}
		return token;
	}

	private static String trim(String value, int maxLength) {
		if (!StringUtils.hasText(value)) return null;
		return value.length() > maxLength ? value.substring(0, maxLength) : value;
	}
}
