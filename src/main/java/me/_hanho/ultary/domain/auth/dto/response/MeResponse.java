package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

	private Long userNo;
	private String loginId;
	private String nickname;
}
