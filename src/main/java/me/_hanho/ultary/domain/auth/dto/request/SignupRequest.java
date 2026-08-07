package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

	@NotBlank(message = "휴대폰 인증 완료 토큰은 필수입니다.")
	private String phoneAuthCompleteToken;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
	private String password;

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
	private String nickname;

	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	private String name;

	@Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
	private String email;
}
