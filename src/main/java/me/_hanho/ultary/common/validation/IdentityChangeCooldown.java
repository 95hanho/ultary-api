package me._hanho.ultary.common.validation;

import java.time.LocalDateTime;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;

/** 검색용 식별자(nickname / mention_id / handle) 변경 쿨다운 */
public final class IdentityChangeCooldown {

	public static final int NICKNAME_DAYS = 7;
	public static final int MENTION_ID_DAYS = 30;
	public static final int HANDLE_DAYS = 30;

	private IdentityChangeCooldown() {
	}

	/**
	 * @param lastChangedAt null이면 아직 한 번도 설정·변경하지 않음 → 즉시 변경 가능
	 */
	public static ChangeAvailabilityResponse availability(LocalDateTime lastChangedAt, int cooldownDays) {
		if (lastChangedAt == null) {
			return ChangeAvailabilityResponse.builder()
					.changeable(true)
					.cooldownDays(cooldownDays)
					.lastChangedAt(null)
					.nextAvailableAt(null)
					.build();
		}
		LocalDateTime nextAvailableAt = lastChangedAt.plusDays(cooldownDays);
		boolean changeable = !LocalDateTime.now().isBefore(nextAvailableAt);
		return ChangeAvailabilityResponse.builder()
				.changeable(changeable)
				.cooldownDays(cooldownDays)
				.lastChangedAt(lastChangedAt)
				.nextAvailableAt(nextAvailableAt)
				.build();
	}

	public static void requireChangeable(
			LocalDateTime lastChangedAt,
			int cooldownDays,
			ErrorCode cooldownError) {
		ChangeAvailabilityResponse availability = availability(lastChangedAt, cooldownDays);
		if (!availability.isChangeable()) {
			throw new BusinessException(cooldownError);
		}
	}
}
