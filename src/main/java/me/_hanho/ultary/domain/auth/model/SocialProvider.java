package me._hanho.ultary.domain.auth.model;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

public enum SocialProvider {
	GOOGLE,
	KAKAO;

	public String nicknamePrefix() {
		return name().toLowerCase() + "_";
	}

	public static SocialProvider from(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_SOCIAL_PROVIDER);
		}
		try {
			return SocialProvider.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_SOCIAL_PROVIDER);
		}
	}
}
