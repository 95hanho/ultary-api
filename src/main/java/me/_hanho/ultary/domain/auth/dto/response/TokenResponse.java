package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private boolean selectionRequired;
    private java.util.List<AccountChoice> accounts;
    public record AccountChoice(Long userNo, String nickname) {}


	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresIn;
	/** 관리자 로그인·재발급만. SUPER | OPERATOR. 회원 토큰은 null */
	private String role;
}
