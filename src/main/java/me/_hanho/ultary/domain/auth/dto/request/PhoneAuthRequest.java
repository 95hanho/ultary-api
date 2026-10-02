package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import me._hanho.ultary.common.validation.PhoneRules;

@Getter
public class PhoneAuthRequest {

	@NotBlank(message = "휴대폰 번호는 필수입니다.")
	@Pattern(regexp = PhoneRules.REGEX, message = PhoneRules.MESSAGE)
	private String phone;

	/**
	 * 비우면 회원가입. {@code PROFILE}은 로그인 회원정보 변경(내 번호는 허용).
	 * {@code PASSWORD}는 비밀번호 재설정이라 이미 가입된 번호로 보낸다.
	 */
	private String purpose;

	public void setPhone(String phone) {
		this.phone = PhoneRules.normalize(phone);
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose == null ? null : purpose.trim();
	}
}
