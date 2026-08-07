package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private long expiresIn;
	/** 이번 요청으로 신규 가입된 경우 true */
	private boolean newUser;
	/** 자동 생성 닉네임이면 true → FE에서 변경 유도 */
	private boolean defaultNickname;
}
