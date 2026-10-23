package me._hanho.ultary.domain.admin.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 상업 태그 설명 거절 멘트. 비우면 저장하지 않는다. */
@Getter
@Setter
public class RejectTagProfileRequest {
	private Long requestId;

	@Size(max = 500, message = "거절 멘트는 500자 이하여야 합니다.")
	private String rejectMessage;
}
