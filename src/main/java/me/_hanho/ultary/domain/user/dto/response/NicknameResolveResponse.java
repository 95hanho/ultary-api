package me._hanho.ultary.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameResolveResponse {

	/** 현재 닉네임태그 */
	private final String nickname;
	/** 요청에 쓴 닉네임태그 */
	private final String requestedNickname;
	/** true면 직전 닉네임(7일 이내)으로 찾아 현재로 바꿔야 함 */
	private final boolean redirected;
}
