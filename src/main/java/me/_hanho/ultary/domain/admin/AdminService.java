package me._hanho.ultary.domain.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.exception.NotImplemented;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.report.ReportService;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;
import me._hanho.ultary.domain.report.model.Report;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserMapper userMapper;
	private final TokenMapper tokenMapper;
	private final ReportService reportService;
	private final FeedService feedService;

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

	public List<ReportResponse> listReports(String status, int limit) {
		return reportService.list(status, limit);
	}

	@Transactional
	public ReportResponse deleteReportedContent(Long reportId) {
		Report report = reportService.requireOpen(reportId);
		switch (report.getTargetType()) {
			case "FEED" -> {
				if (report.getTargetFeedId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
				feedService.deleteByAdmin(report.getTargetFeedId());
			}
			case "COMMENT" -> {
				if (report.getTargetCommentId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
				feedService.deleteCommentByAdmin(report.getTargetCommentId());
			}
			case "REPLY" -> {
				if (report.getTargetReplyId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
				feedService.deleteReplyByAdmin(report.getTargetReplyId());
			}
			default -> throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
		}
		return reportService.close(reportId, ReportService.DELETED);
	}

	@Transactional
	public ReportResponse suspendReportedUser(Long reportId) {
		Report report = reportService.requireOpen(reportId);
		if (!"USER".equals(report.getTargetType()) || report.getTargetUserNo() == null) {
			throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
		}
		User user = requireUser(report.getTargetUserNo());
		if ("WITHDRAWN".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.CANNOT_SUSPEND_WITHDRAWN);
		}
		if (!"SUSPENDED".equals(user.getWithdrawalStatus())) {
			if (userMapper.suspend(user.getUserNo()) == 0) {
				throw new BusinessException(ErrorCode.USER_INACTIVE);
			}
			tokenMapper.revokeAllByUserNo(user.getUserNo());
		}
		return reportService.close(reportId, ReportService.SUSPENDED);
	}

	@Transactional
	public ReportResponse rejectReport(Long reportId) {
		reportService.requireOpen(reportId);
		return reportService.close(reportId, ReportService.REJECTED);
	}

	@Transactional
	public ReportResponse holdReport(Long reportId) {
		reportService.requireOpen(reportId);
		return reportService.close(reportId, ReportService.ON_HOLD);
	}

	@Transactional
	public ReportResponse confirmReport(Long reportId) {
		return reportService.confirm(reportId);
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
