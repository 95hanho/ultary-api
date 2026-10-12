package me._hanho.ultary.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLoginRequest {

	@NotBlank(message = "아이디는 필수입니다.")
	@Size(max = 200, message = "아이디는 200자 이하여야 합니다.")
	private String loginId;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;
}
