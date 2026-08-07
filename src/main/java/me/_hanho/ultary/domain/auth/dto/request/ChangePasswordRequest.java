package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

	@NotBlank(message = "비밀번호 변경 토큰은 필수입니다.")
	private String passwordChangeToken;

	@NotBlank(message = "새 비밀번호는 필수입니다.")
	@Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
	private String newPassword;
}
