package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.ValidNickname;

@Getter
@Setter
public class ChangeNicknameRequest {

	@NotBlank(message = "닉네임은 필수입니다.")
	@ValidNickname
	private String nickname;
}
