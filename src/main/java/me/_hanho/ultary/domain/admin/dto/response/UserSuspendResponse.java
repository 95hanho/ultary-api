package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSuspendResponse {

	private Long userNo;
	private String withdrawalStatus;
	private LocalDateTime suspendedAt;
}
