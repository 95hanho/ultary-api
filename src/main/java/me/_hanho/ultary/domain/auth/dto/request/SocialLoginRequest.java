package me._hanho.ultary.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FE/BFF가 소셜 OAuth 후 전달하는 식별 정보.
 * BE는 제공자 토큰을 검증하지 않고 DB 조회/가입만 수행한다.
 */
@Getter
@Setter
public class SocialLoginRequest {

	@NotBlank(message = "소셜 제공자는 필수입니다.")
	private String provider;

	@NotBlank(message = "소셜 사용자 ID는 필수입니다.")
	@Size(max = 100, message = "소셜 사용자 ID는 100자 이하여야 합니다.")
	private String providerUserId;

	@Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
	private String email;

	/** OAuth 표시명은 DB name(20)보다 길 수 있음 → 서비스에서 truncate */
	@Size(max = 100, message = "이름은 100자 이하여야 합니다.")
	private String name;
}
