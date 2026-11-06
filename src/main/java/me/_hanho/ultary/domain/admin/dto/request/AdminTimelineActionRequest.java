package me._hanho.ultary.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTimelineActionRequest {
	/** STORY_DELETED | FEED_DELETED | PASSWORD_CHANGED | NICKNAME_CHANGED | PET_DELETED | ACCOUNT_WITHDRAWN */
	@NotBlank
	private String type;
	/** 대상 ID. 비밀번호·닉네임·탈퇴는 userNo와 같아야 함 */
	@NotNull
	private Long id;
	@Size(max = 500)
	private String reason;
}
