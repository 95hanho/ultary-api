package me._hanho.ultary.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String CLAIM_OWNER_TYPE = "ownerType";
	private static final String CLAIM_TOKEN_TYPE = "tokenType";
	private static final String CLAIM_PHONE = "phone";

	private final JwtProperties jwtProperties;

	public String createUserAccessToken(Long userNo) {
		return createToken(
				String.valueOf(userNo),
				TokenOwnerType.USER,
				JwtTokenType.ACCESS,
				jwtProperties.getSecret().getUser(),
				jwtProperties.getExpiration().getAccess(),
				null);
	}

	public String createRefreshToken(Long ownerNo, TokenOwnerType ownerType) {
		return createToken(
				String.valueOf(ownerNo),
				ownerType,
				JwtTokenType.REFRESH,
				jwtProperties.getSecret().getRefresh(),
				jwtProperties.getExpiration().getRefresh(),
				null);
	}

	public String createPhoneAuthToken(String phone) {
		Map<String, Object> extra = new HashMap<>();
		extra.put(CLAIM_PHONE, phone);
		return createToken(
				phone,
				null,
				JwtTokenType.PHONE_AUTH,
				jwtProperties.getSecret().getPhoneauth(),
				jwtProperties.getExpiration().getPhoneauth(),
				extra);
	}

	public String createPhoneAuthCompleteToken(String phone) {
		Map<String, Object> extra = new HashMap<>();
		extra.put(CLAIM_PHONE, phone);
		return createToken(
				phone,
				null,
				JwtTokenType.PHONE_AUTH_COMPLETE,
				jwtProperties.getSecret().getPhoneauthComplete(),
				jwtProperties.getExpiration().getPhoneauthComplete(),
				extra);
	}

	public String createPasswordChangeToken(Long userNo) {
		return createToken(
				String.valueOf(userNo),
				TokenOwnerType.USER,
				JwtTokenType.PASSWORD_CHANGE,
				jwtProperties.getSecret().getPwdchange(),
				jwtProperties.getExpiration().getPwdchange(),
				null);
	}

	public long getAccessTokenExpiresInSeconds() {
		return jwtProperties.getExpiration().getAccess();
	}

	public long getRefreshTokenExpiresInSeconds() {
		return jwtProperties.getExpiration().getRefresh();
	}

	public Claims parseUserAccessToken(String token) {
		Claims claims = parseClaims(token, jwtProperties.getSecret().getUser());
		validateClaims(claims, TokenOwnerType.USER, JwtTokenType.ACCESS);
		return claims;
	}

	public Claims parseRefreshToken(String token) {
		Claims claims = parseClaims(token, jwtProperties.getSecret().getRefresh());
		validateClaims(claims, null, JwtTokenType.REFRESH);
		return claims;
	}

	public Claims parsePhoneAuthToken(String token) {
		Claims claims = parseClaims(token, jwtProperties.getSecret().getPhoneauth());
		validateClaims(claims, null, JwtTokenType.PHONE_AUTH);
		return claims;
	}

	public Claims parsePhoneAuthCompleteToken(String token) {
		Claims claims = parseClaims(token, jwtProperties.getSecret().getPhoneauthComplete());
		validateClaims(claims, null, JwtTokenType.PHONE_AUTH_COMPLETE);
		return claims;
	}

	public Claims parsePasswordChangeToken(String token) {
		Claims claims = parseClaims(token, jwtProperties.getSecret().getPwdchange());
		validateClaims(claims, TokenOwnerType.USER, JwtTokenType.PASSWORD_CHANGE);
		return claims;
	}

	public Long getSubjectAsLong(Claims claims) {
		return Long.valueOf(claims.getSubject());
	}

	public String getPhone(Claims claims) {
		String phone = claims.get(CLAIM_PHONE, String.class);
		if (phone == null || phone.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		return phone;
	}

	public TokenOwnerType getOwnerType(Claims claims) {
		return TokenOwnerType.valueOf(claims.get(CLAIM_OWNER_TYPE, String.class));
	}

	private String createToken(
			String subject,
			TokenOwnerType ownerType,
			JwtTokenType tokenType,
			String secret,
			long expirationSeconds,
			Map<String, Object> extraClaims) {
		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + (expirationSeconds * 1000));

		var builder = Jwts.builder()
				.setSubject(subject)
				.claim(CLAIM_TOKEN_TYPE, tokenType.name())
				.setIssuedAt(now)
				.setExpiration(expiresAt);

		if (ownerType != null) {
			builder.claim(CLAIM_OWNER_TYPE, ownerType.name());
		}
		if (extraClaims != null) {
			extraClaims.forEach(builder::claim);
		}

		return builder
				.signWith(secretKey(secret), SignatureAlgorithm.HS256)
				.compact();
	}

	private Claims parseClaims(String token, String secret) {
		try {
			return Jwts.parserBuilder()
					.setSigningKey(secretKey(secret))
					.build()
					.parseClaimsJws(token)
					.getBody();
		} catch (ExpiredJwtException ex) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		} catch (MalformedJwtException | UnsupportedJwtException | SignatureException | IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

	private void validateClaims(Claims claims, TokenOwnerType expectedOwnerType, JwtTokenType expectedTokenType) {
		String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
		if (tokenType == null || !expectedTokenType.name().equals(tokenType)) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		if (expectedOwnerType != null) {
			String ownerType = claims.get(CLAIM_OWNER_TYPE, String.class);
			if (ownerType == null || !expectedOwnerType.name().equals(ownerType)) {
				throw new BusinessException(ErrorCode.INVALID_TOKEN);
			}
		}
	}

	private SecretKey secretKey(String secret) {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
}
