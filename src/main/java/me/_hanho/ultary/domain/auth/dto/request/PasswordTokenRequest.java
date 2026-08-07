package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordTokenRequest {

	@NotBlank(message = "휴대폰 번호는 필수입니다.")
	@Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
	private String phone;

	@NotBlank(message = "휴대폰 인증 완료 토큰은 필수입니다.")
	private String phoneAuthCompleteToken;
}
