package me._hanho.ultary.domain.report;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.feed.model.FeedComment;
import me._hanho.ultary.domain.feed.model.FeedReply;
import me._hanho.ultary.domain.report.dto.request.CreateReportRequest;
import me._hanho.ultary.domain.report.dto.response.MyReportResponse;
import me._hanho.ultary.domain.report.dto.response.ReportResponse;
import me._hanho.ultary.domain.report.model.Report;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

	public static final Set<String> REASONS = Set.of(
			"SPAM",
			"ABUSE",
			"HARASSMENT",
			"SEXUAL",
			"VIOLENCE",
			"HATE",
			"IMPERSONATION",
			"PRIVACY",
			"OTHER");

	public static final String REQUESTED = "REQUESTED";
	public static final String CONFIRMED = "CONFIRMED";
	public static final String DELETED = "DELETED";
	public static final String REJECTED = "REJECTED";
	public static final String ON_HOLD = "ON_HOLD";
	public static final String SUSPENDED = "SUSPENDED";
	public static final Set<String> STATUSES = Set.of(REQUESTED, CONFIRMED, DELETED, REJECTED, ON_HOLD, SUSPENDED);
	public static final Set<String> OPEN_STATUSES = Set.of(REQUESTED, CONFIRMED, ON_HOLD);

	public static final Set<String> TARGET_TYPES = Set.of("USER", "FEED", "COMMENT", "REPLY");

	private final ReportMapper reportMapper;
	private final UserMapper userMapper;
	private final FeedMapper feedMapper;

	@Transactional
	public ReportResponse create(UserPrincipal principal, CreateReportRequest request) {
		String targetType = normalize(request.getTargetType());
		String reason = normalize(request.getReason());
		if (!TARGET_TYPES.contains(targetType)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 대상은 USER, FEED, COMMENT, REPLY 입니다.");
		}
		if (!REASONS.contains(reason)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 사유가 올바르지 않습니다.");
		}
		if (request.getTargetId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 대상이 필요합니다.");
		}

		Report report = new Report();
		report.setReporterUserNo(principal.getUserNo());
		report.setTargetType(targetType);
		report.setReason(reason);
		fillTarget(report, principal.getUserNo(), targetType, request.getTargetId());

		Report opened = reportMapper.findOpenByReporterAndTarget(
				principal.getUserNo(),
				targetType,
				report.getTargetUserNo(),
				"FEED".equals(targetType) ? report.getTargetFeedId() : null,
				"COMMENT".equals(targetType) ? report.getTargetCommentId() : null,
				"REPLY".equals(targetType) ? report.getTargetReplyId() : null);
		if (opened != null) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_OPENED);
		}

		reportMapper.insert(report);
		log.info("[create] reportId={} type={} reason={} by={}",
				report.getReportId(), targetType, reason, principal.getUserNo());
		return toResponse(reportMapper.findById(report.getReportId()));
	}

	public List<ReportResponse> list(String status, int limit) {
		String normalized = status == null || status.isBlank() ? REQUESTED : status.trim().toUpperCase();
		if (!STATUSES.contains(normalized)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 상태가 올바르지 않습니다.");
		}
		int size = Math.min(Math.max(limit, 1), 50);
		return reportMapper.findList(normalized, size).stream().map(this::toResponse).toList();
	}

	public Report requireOpen(Long reportId) {
		Report report = reportMapper.findById(reportId);
		if (report == null) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
		}
		if (!OPEN_STATUSES.contains(report.getStatus())) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_CLOSED);
		}
		return report;
	}

	@Transactional
	public ReportResponse close(Long reportId, String status) {
		if (reportMapper.close(reportId, status) == 0) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_CLOSED);
		}
		return toResponse(reportMapper.findById(reportId));
	}

	@Transactional
	public ReportResponse confirm(Long reportId) {
		Report report = reportMapper.findById(reportId);
		if (report == null) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
		}
		if (!REQUESTED.equals(report.getStatus())) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_CLOSED);
		}
		if (reportMapper.confirm(reportId) == 0) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_CLOSED);
		}
		return toResponse(reportMapper.findById(reportId));
	}

	@Transactional
	public void cancel(UserPrincipal principal, Long reportId) {
		Report report = reportMapper.findById(reportId);
		if (report == null || !principal.getUserNo().equals(report.getReporterUserNo())) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
		}
		if (!REQUESTED.equals(report.getStatus())) {
			throw new BusinessException(ErrorCode.REPORT_CANNOT_CANCEL);
		}
		if (reportMapper.deleteOwnRequested(reportId, principal.getUserNo()) == 0) {
			throw new BusinessException(ErrorCode.REPORT_CANNOT_CANCEL);
		}
		log.info("[cancel] reportId={} by={}", reportId, principal.getUserNo());
	}

	public Map<Long, MyReportResponse> mineFeeds(Long reporterUserNo, Collection<Long> feedIds) {
		return mine(reporterUserNo, feedIds, reportMapper::findMineByFeedIds, Report::getTargetFeedId);
	}

	public Map<Long, MyReportResponse> mineComments(Long reporterUserNo, Collection<Long> commentIds) {
		return mine(reporterUserNo, commentIds, reportMapper::findMineByCommentIds, Report::getTargetCommentId);
	}

	public Map<Long, MyReportResponse> mineReplies(Long reporterUserNo, Collection<Long> replyIds) {
		return mine(reporterUserNo, replyIds, reportMapper::findMineByReplyIds, Report::getTargetReplyId);
	}

	public Map<Long, MyReportResponse> mineUsers(Long reporterUserNo, Collection<Long> userNos) {
		return mine(reporterUserNo, userNos, reportMapper::findMineByUserNos, Report::getTargetUserNo);
	}

	public MyReportResponse mineFeed(Long reporterUserNo, Long feedId) {
		return one(mineFeeds(reporterUserNo, feedId == null ? List.of() : List.of(feedId)), feedId);
	}

	public MyReportResponse mineComment(Long reporterUserNo, Long commentId) {
		return one(mineComments(reporterUserNo, commentId == null ? List.of() : List.of(commentId)), commentId);
	}

	public MyReportResponse mineReply(Long reporterUserNo, Long replyId) {
		return one(mineReplies(reporterUserNo, replyId == null ? List.of() : List.of(replyId)), replyId);
	}

	public MyReportResponse mineUser(Long reporterUserNo, Long userNo) {
		return one(mineUsers(reporterUserNo, userNo == null ? List.of() : List.of(userNo)), userNo);
	}

	private MyReportResponse one(Map<Long, MyReportResponse> reports, Long id) {
		return id == null ? null : reports.get(id);
	}

	private Map<Long, MyReportResponse> mine(
			Long reporterUserNo,
			Collection<Long> targetIds,
			MineQuery query,
			Function<Report, Long> targetId) {
		if (reporterUserNo == null || targetIds == null || targetIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Report> chosen = new HashMap<>();
		for (Report row : query.find(reporterUserNo, targetIds)) {
			Long id = targetId.apply(row);
			if (id == null) {
				continue;
			}
			chosen.merge(id, row, this::prefer);
		}
		Map<Long, MyReportResponse> result = new HashMap<>();
		chosen.forEach((id, row) -> result.put(id, MyReportResponse.builder()
				.reportId(row.getReportId())
				.status(row.getStatus())
				.build()));
		return result;
	}

	/** 처리 중인 신고를 우선하고, 같으면 더 최근 신고를 고른다. */
	private Report prefer(Report current, Report next) {
		boolean currentOpen = OPEN_STATUSES.contains(current.getStatus());
		boolean nextOpen = OPEN_STATUSES.contains(next.getStatus());
		if (nextOpen != currentOpen) {
			return nextOpen ? next : current;
		}
		return next.getReportId() > current.getReportId() ? next : current;
	}

	@FunctionalInterface
	private interface MineQuery {
		List<Report> find(Long reporterUserNo, Collection<Long> targetIds);
	}

	private void fillTarget(Report report, Long reporterUserNo, String targetType, Long targetId) {
		switch (targetType) {
			case "USER" -> fillUser(report, reporterUserNo, targetId);
			case "FEED" -> fillFeed(report, reporterUserNo, targetId);
			case "COMMENT" -> fillComment(report, reporterUserNo, targetId);
			case "REPLY" -> fillReply(report, reporterUserNo, targetId);
			default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "신고 대상은 USER, FEED, COMMENT, REPLY 입니다.");
		}
	}

	private void fillUser(Report report, Long reporterUserNo, Long userNo) {
		if (reporterUserNo.equals(userNo)) {
			throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
		}
		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		report.setTargetUserNo(userNo);
	}

	private void fillFeed(Report report, Long reporterUserNo, Long feedId) {
		Feed feed = feedMapper.findActiveByFeedId(feedId);
		if (feed == null) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		if (reporterUserNo.equals(feed.getUserNo())) {
			throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
		}
		report.setTargetFeedId(feedId);
	}

	private void fillComment(Report report, Long reporterUserNo, Long commentId) {
		FeedComment comment = feedMapper.findActiveCommentById(commentId);
		if (comment == null) {
			throw new BusinessException(ErrorCode.FEED_COMMENT_NOT_FOUND);
		}
		if (reporterUserNo.equals(comment.getUserNo())) {
			throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
		}
		report.setTargetCommentId(commentId);
		report.setTargetFeedId(comment.getFeedId());
	}

	private void fillReply(Report report, Long reporterUserNo, Long replyId) {
		FeedReply reply = feedMapper.findActiveReplyById(replyId);
		if (reply == null) {
			throw new BusinessException(ErrorCode.FEED_REPLY_NOT_FOUND);
		}
		if (reporterUserNo.equals(reply.getUserNo())) {
			throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
		}
		report.setTargetReplyId(replyId);
		report.setTargetCommentId(reply.getFeedCommentId());
		FeedComment comment = feedMapper.findActiveCommentById(reply.getFeedCommentId());
		if (comment != null) {
			report.setTargetFeedId(comment.getFeedId());
		}
	}

	private ReportResponse toResponse(Report report) {
		return ReportResponse.builder()
				.reportId(report.getReportId())
				.reporterUserNo(report.getReporterUserNo())
				.targetType(report.getTargetType())
				.targetUserNo(report.getTargetUserNo())
				.targetFeedId(report.getTargetFeedId())
				.targetCommentId(report.getTargetCommentId())
				.targetReplyId(report.getTargetReplyId())
				.reason(report.getReason())
				.status(report.getStatus())
				.createdAt(report.getCreatedAt())
				.processedAt(report.getProcessedAt())
				.build();
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toUpperCase();
	}
}
