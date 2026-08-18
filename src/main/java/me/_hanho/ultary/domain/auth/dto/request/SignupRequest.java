package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.PasswordRules;
import me._hanho.ultary.common.validation.ValidNickname;

@Getter
@Setter
public class SignupRequest {

	@NotBlank(message = "휴대폰 인증 완료 토큰은 필수입니다.")
	private String phoneAuthCompleteToken;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
	@Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
	private String password;

	@NotBlank(message = "닉네임은 필수입니다.")
	@ValidNickname
	private String nickname;

	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	private String name;

	@Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
	private String email;
}
