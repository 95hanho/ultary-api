package me._hanho.ultary.common.validation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import me._hanho.ultary.domain.auth.dto.request.SignupRequest;

class NicknameTagValidationTest {
    @Test void requestAllowsOnlyOneToTenLowercaseLettersOrUnderscores() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new SignupRequest();
            for (String name : new String[]{"a", "_", "nari_owner", "abcdefghij"}) {
                request.setNickname(name);
                assertTrue(validator.validateProperty(request, "nickname").isEmpty(), name);
            }
            for (String name : new String[]{"", " ", "Upper", "user1", "울타리", "@ultary", "two words", "a-b", "abcdefghijk", " ultary "}) {
                request.setNickname(name);
                assertFalse(validator.validateProperty(request, "nickname").isEmpty(), name);
            }
        }
    }
}
