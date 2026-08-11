package me._hanho.ultary.domain.test.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.PasswordRules;

@Getter
@Setter
public class PasswordEncodeRequest {

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
	@Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
	private String password;
}
