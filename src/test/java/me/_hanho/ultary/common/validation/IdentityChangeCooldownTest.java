package me._hanho.ultary.common.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

class IdentityChangeCooldownTest {
    @Test void nicknameMustWaitThirtyDaysIncludingThePreviouslyAllowedWeek() {
        assertEquals(30, IdentityChangeCooldown.NICKNAME_DAYS);
        for (int days : new int[]{7, 15, 29}) {
            var changed = LocalDateTime.now().minusDays(days);
            var result = IdentityChangeCooldown.availability(changed, IdentityChangeCooldown.NICKNAME_DAYS);
            assertFalse(result.isChangeable());
            assertEquals(30, result.getCooldownDays());
            assertEquals(changed.plusDays(30), result.getNextAvailableAt());
            var error = assertThrows(BusinessException.class, () -> IdentityChangeCooldown.requireChangeable(
                changed, IdentityChangeCooldown.NICKNAME_DAYS, ErrorCode.NICKNAME_CHANGE_COOLDOWN));
            assertEquals(ErrorCode.NICKNAME_CHANGE_COOLDOWN, error.getErrorCode());
        }
    }

    @Test void nicknameCanChangeAfterThirtyDaysOrWithoutAnEarlierChange() {
        var elapsed = LocalDateTime.now().minusDays(30).minusSeconds(1);
        assertTrue(IdentityChangeCooldown.availability(elapsed, IdentityChangeCooldown.NICKNAME_DAYS).isChangeable());
        assertDoesNotThrow(() -> IdentityChangeCooldown.requireChangeable(
            elapsed, IdentityChangeCooldown.NICKNAME_DAYS, ErrorCode.NICKNAME_CHANGE_COOLDOWN));
        assertTrue(IdentityChangeCooldown.availability(null, IdentityChangeCooldown.NICKNAME_DAYS).isChangeable());
    }
}
