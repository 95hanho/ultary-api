package me._hanho.ultary.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTimelineActionResponse {
	private String type;
	private Long id;
	/** 비밀번호 초기화 시에만 1회 평문. 그 외 null */
	private String temporaryPassword;
}
