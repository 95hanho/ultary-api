package me._hanho.ultary.domain.test.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResetResponse {

	private Long userNo;
	/** 폐기한 ultary_token(리프레시) 행 수 */
	private int revokedCount;
}
