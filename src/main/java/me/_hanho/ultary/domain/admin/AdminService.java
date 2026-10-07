package me._hanho.ultary.domain.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.exception.NotImplemented;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserMapper userMapper;
	private final TokenMapper tokenMapper;

	public void approveTag(Long tagId) {
		NotImplemented.yet();
	}

	public void rejectTag(Long tagId) {
		NotImplemented.yet();
	}

	@Transactional
	public UserSuspendResponse suspendUser(Long userNo) {
		User user = requireUser(userNo);
		if ("WITHDRAWN".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.CANNOT_SUSPEND_WITHDRAWN);
		}
		if ("SUSPENDED".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_SUSPENDED);
		}
		if (userMapper.suspend(userNo) == 0) {
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		}
		tokenMapper.revokeAllByUserNo(userNo);
		return toResponse(userMapper.findByUserNo(userNo));
	}

	@Transactional
	public UserSuspendResponse unsuspendUser(Long userNo) {
		User user = requireUser(userNo);
		if (!"SUSPENDED".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_SUSPENDED);
		}
		if (userMapper.unsuspend(userNo) == 0) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_SUSPENDED);
		}
		return toResponse(userMapper.findByUserNo(userNo));
	}

	private User requireUser(Long userNo) {
		User user = userMapper.findByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		return user;
	}

	private UserSuspendResponse toResponse(User user) {
		return UserSuspendResponse.builder()
				.userNo(user.getUserNo())
				.withdrawalStatus(user.getWithdrawalStatus())
				.suspendedAt(user.getSuspendedAt())
				.build();
	}
}
