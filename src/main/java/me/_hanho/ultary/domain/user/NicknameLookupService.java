package me._hanho.ultary.domain.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.user.dto.response.NicknameResolveResponse;
import me._hanho.ultary.domain.user.model.User;

/**
 * 현재·직전(7일) 닉네임태그 조회.
 * 변경 쿨다운(30일)과 별도로, 직전 닉네임 선점·구 URL 리다이렉트만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class NicknameLookupService {

	private final UserMapper userMapper;

	@Transactional(readOnly = true)
	public User findActiveByCurrentOrPrevious(String nickname) {
		if (!StringUtils.hasText(nickname)) {
			return null;
		}
		String trimmed = nickname.trim();
		User current = userMapper.findActiveByNickname(trimmed);
		if (current != null) {
			return current;
		}
		return userMapper.findActiveByPreviousNicknameWithinDays(
				trimmed, IdentityChangeCooldown.PREVIOUS_NICKNAME_RESERVE_DAYS);
	}

	@Transactional(readOnly = true)
	public NicknameResolveResponse resolve(String nickname) {
		if (!StringUtils.hasText(nickname)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		String requested = nickname.trim();
		User user = findActiveByCurrentOrPrevious(requested);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		boolean redirected = !requested.equals(user.getNickname());
		return NicknameResolveResponse.builder()
				.nickname(user.getNickname())
				.requestedNickname(requested)
				.redirected(redirected)
				.build();
	}

	public boolean isNicknameAvailable(String nickname, Long excludeUserNo) {
		if (!StringUtils.hasText(nickname)) {
			return false;
		}
		return userMapper.countByNickname(
				nickname.trim(),
				excludeUserNo,
				IdentityChangeCooldown.PREVIOUS_NICKNAME_RESERVE_DAYS) == 0;
	}
}
