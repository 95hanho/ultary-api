package me._hanho.ultary.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhoneVerifyResponse {

	private String phoneAuthCompleteToken;
}
