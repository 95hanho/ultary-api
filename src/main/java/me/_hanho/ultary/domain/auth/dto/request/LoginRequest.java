package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * email 또는 phone 중 하나로 로그인 (비밀번호 설정 사용자).
 */
@Getter
@Setter
public class LoginRequest {

	private String email;

	private String phone;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;

	@AssertTrue(message = "이메일 또는 휴대폰 번호 중 하나는 필수입니다.")
	public boolean isIdentifierPresent() {
		return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
	}
}
