package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.PasswordRules;

/**
 * 로그인 상태에서 비밀번호 변경/최초 설정.
 * 이미 비밀번호가 있으면 currentPassword 필수.
 */
@Getter
@Setter
public class ChangeMyPasswordRequest {

	/** 기존 비밀번호 (소셜만 사용 중이라 password null이면 생략) */
	private String currentPassword;

	@NotBlank(message = "새 비밀번호는 필수입니다.")
	@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
	@Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
	private String newPassword;
}
