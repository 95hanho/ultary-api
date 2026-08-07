package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMeRequest {

	@Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
	private String nickname;

	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	private String name;

	@Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
	private String email;

	@Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
	private String bio;

	@Size(max = 30, message = "시/도는 30자 이하여야 합니다.")
	private String regionSido;

	@Size(max = 30, message = "시/군/구는 30자 이하여야 합니다.")
	private String regionSigungu;
}
