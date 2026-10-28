package me._hanho.ultary.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PetNameConstraintValidator implements ConstraintValidator<ValidPetName, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 필수 입력은 @NotBlank에서 검사한다.
        return value == null || value.isBlank() || PetNameRules.isValid(value);
    }
}
