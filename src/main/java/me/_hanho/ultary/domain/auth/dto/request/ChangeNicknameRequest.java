package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.NicknameRules;

@Getter
@Setter
public class ChangeNicknameRequest {

	@NotBlank(message = "닉네임은 필수입니다.")
	@Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
	@Pattern(regexp = NicknameRules.REGEX, message = NicknameRules.MESSAGE)
	private String nickname;
}
