package me._hanho.ultary.common.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;

class PetNameValidationTest {
    @Test
    void validatesWeightedBoundariesOnTheRequestField() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new CreatePetRequest();
            for (String name : new String[]{"A", "초코", "a".repeat(30), "가".repeat(15), "가".repeat(14) + "AB"}) {
                request.setName(name);
                assertTrue(validator.validateProperty(request, "name").isEmpty(), name);
            }
            for (String name : new String[]{"", " ", "a".repeat(31), "가".repeat(16), "가".repeat(15) + "A", "초코1", "초 코", "Coco_", "코코🐶", "ㄱ", "é"}) {
                request.setName(name);
                assertFalse(validator.validateProperty(request, "name").isEmpty(), name);
            }
            request.setName(null);
            assertFalse(validator.validateProperty(request, "name").isEmpty());
        }
    }
}
