package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailabilityCheckResponse {

	/** true면 사용 가능 */
	private boolean available;
}
