package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneVerifyRequest {

	@NotBlank(message = "휴대폰 인증 토큰은 필수입니다.")
	private String phoneAuthToken;

	@NotBlank(message = "인증번호는 필수입니다.")
	private String code;
}
