package me._hanho.ultary.domain.test.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PasswordEncodeResponse {

	/** PasswordEncoder로 해시된 값 (DB password 컬럼에 넣을 문자열) */
	private String encodedPassword;
}
