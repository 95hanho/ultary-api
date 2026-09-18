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

	public void setPhone(String phone) {
		this.phone = PhoneRules.normalize(phone);
	}
}
