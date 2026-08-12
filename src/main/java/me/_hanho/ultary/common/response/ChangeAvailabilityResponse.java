package me._hanho.ultary.common.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangeAvailabilityResponse {

	private boolean changeable;
	private int cooldownDays;
	private LocalDateTime lastChangedAt;
	/** changeable=false일 때 다음 변경 가능 시각. 아직 미설정(null last)이면 null */
	private LocalDateTime nextAvailableAt;
}
