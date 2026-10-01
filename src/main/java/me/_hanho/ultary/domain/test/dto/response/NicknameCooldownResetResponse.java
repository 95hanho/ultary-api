package me._hanho.ultary.domain.test.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameCooldownResetResponse {

	private Long userNo;
	/** 쿨다운 기준. 올해 1월 1일 00:00 */
	private LocalDateTime nicknameChangedAt;
}
