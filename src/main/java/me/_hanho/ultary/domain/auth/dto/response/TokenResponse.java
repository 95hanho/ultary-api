package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresIn;
}
