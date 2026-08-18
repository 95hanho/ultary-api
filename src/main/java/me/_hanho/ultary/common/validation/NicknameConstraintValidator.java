package me._hanho.ultary.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NicknameConstraintValidator implements ConstraintValidator<ValidNickname, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		return NicknameRules.isValid(value.trim());
	}
}
