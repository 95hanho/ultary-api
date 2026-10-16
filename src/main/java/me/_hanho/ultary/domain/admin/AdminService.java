package me._hanho.ultary.domain.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.admin.dto.response.AdminMenuCountResponse;
import me._hanho.ultary.domain.admin.dto.response.UserSuspendResponse;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.report.ReportService;
import me._hanho.ultary.domain.report.dto.response.ReportPageResponse;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;
import me._hanho.ultary.domain.report.model.Report;
import me._hanho.ultary.domain.tag.TagService;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestPageResponse;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestResponse;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final UserMapper userMapper;
	private final TokenMapper tokenMapper;
	private final ReportService reportService;
	private final FeedService feedService;
	private final TagService tagService;

	@Transactional(readOnly = true)
	public AdminMenuCountResponse menuCounts() {
		return AdminMenuCountResponse.builder()
				.pendingTagRequestCount(tagService.countPendingProfileRequests())
				.requestedReportCount(reportService.countRequested())
				.build();
	}

	public TagProfileRequestPageResponse listTagProfileRequests(String status, Long cursorRequestId, int limit) {
		return tagService.listProfileRequestsForAdmin(status, cursorRequestId, limit);
	}

	public TagProfileRequestResponse approveTag(Long adminNo, Long tagId) {
		return tagService.approveProfile(adminNo, tagId);
	}

	public TagProfileRequestResponse rejectTag(Long adminNo, Long tagId, String rejectMessage) {
		return tagService.rejectProfile(adminNo, tagId, rejectMessage);
	}

	public TagProfileRequestResponse reopenTag(Long tagId) {
		return tagService.reopenProfile(tagId);
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

	public ReportPageResponse listReports(String status, Long cursorReportId, int limit) {
		return reportService.list(status, cursorReportId, limit);
	}

	public ReportResponse getReport(Long reportId) {
		return reportService.get(reportId);
	}

	@Transactional
	public ReportResponse deleteReportedContent(Long reportId) {
		Report report = reportService.requireExists(reportId);
		switch (report.getTargetType()) {
			case "FEED" -> {
				if (report.getTargetFeedId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
			}
			case "COMMENT" -> {
				if (report.getTargetCommentId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
			}
			case "REPLY" -> {
				if (report.getTargetReplyId() == null) {
					throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
				}
			}
			default -> throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
		}
		undoReportSideEffects(report);
		switch (report.getTargetType()) {
			case "FEED" -> feedService.deleteByAdmin(report.getTargetFeedId());
			case "COMMENT" -> feedService.deleteCommentByAdmin(report.getTargetCommentId());
			case "REPLY" -> feedService.deleteReplyByAdmin(report.getTargetReplyId());
			default -> {
			}
		}
		return reportService.close(reportId, ReportService.DELETED);
	}

	@Transactional
	public ReportResponse suspendReportedUser(Long reportId) {
		Report report = reportService.requireExists(reportId);
		if (!"USER".equals(report.getTargetType()) || report.getTargetUserNo() == null) {
			throw new BusinessException(ErrorCode.REPORT_ACTION_MISMATCH);
		}
		User user = requireUser(report.getTargetUserNo());
		if ("WITHDRAWN".equals(user.getWithdrawalStatus())) {
			throw new BusinessException(ErrorCode.CANNOT_SUSPEND_WITHDRAWN);
		}
		undoReportSideEffects(report);
		user = requireUser(report.getTargetUserNo());
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
		Report report = reportService.requireExists(reportId);
		undoReportSideEffects(report);
		return reportService.close(reportId, ReportService.REJECTED);
	}

	@Transactional
	public ReportResponse confirmReport(Long reportId) {
		Report report = reportService.requireExists(reportId);
		undoReportSideEffects(report);
		return reportService.close(reportId, ReportService.CONFIRMED);
	}

	/**
	 * 이전 조치의 부수 효과를 되돌린다.
	 * 정지→다른 상태면 회원 정지 해제, 삭제조치→다른 상태면 글·댓글·답글 복구.
	 * 같은 대상에 같은 상태 신고가 더 있으면 유지한다.
	 */
	private void undoReportSideEffects(Report report) {
		if (ReportService.SUSPENDED.equals(report.getStatus())
				&& "USER".equals(report.getTargetType())
				&& report.getTargetUserNo() != null
				&& !reportService.hasOtherWithStatus(report, ReportService.SUSPENDED)) {
			User user = userMapper.findByUserNo(report.getTargetUserNo());
			if (user != null && "SUSPENDED".equals(user.getWithdrawalStatus())) {
				userMapper.unsuspend(user.getUserNo());
			}
		}
		if (!ReportService.DELETED.equals(report.getStatus())
				|| reportService.hasOtherWithStatus(report, ReportService.DELETED)) {
			return;
		}
		switch (report.getTargetType()) {
			case "FEED" -> {
				if (report.getTargetFeedId() != null) {
					feedService.restoreByAdmin(report.getTargetFeedId());
				}
			}
			case "COMMENT" -> {
				if (report.getTargetCommentId() != null) {
					feedService.restoreCommentByAdmin(report.getTargetCommentId(), report.getTargetFeedId());
				}
			}
			case "REPLY" -> {
				if (report.getTargetReplyId() != null) {
					feedService.restoreReplyByAdmin(report.getTargetReplyId(), report.getTargetFeedId());
				}
			}
			default -> {
			}
		}
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
