package me._hanho.ultary.domain.ai;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.domain.ai.model.*;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.domain.ws.RealtimePush;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationTransactions {
    private final ModerationMapper mapper;
    private final FeedMapper feedMapper;
    private final FeedService feedService;
    private final me._hanho.ultary.domain.feed.CommentMentionService commentMentions;
    private final NotificationService notifications;
    private final RealtimePush realtime;

    @Transactional
    public ModerationJob claim(Long id) {
        String token = UUID.randomUUID().toString();
        return mapper.claim(id, token) == 1 ? mapper.findJob(id) : null;
    }

    @Transactional
    public void complete(ModerationJob claimed, ModerationResult result) {
        // Same lock order as content writes: feed, child, state, job.
        ModerationTarget feed = mapper.lockTarget("FEED", claimed.getFeedId());
        ModerationTarget target = "FEED".equals(claimed.getTargetType()) ? feed
                : mapper.lockTarget(claimed.getTargetType(), claimed.getTargetId());
        ModerationState state = mapper.lockState(claimed.getTargetType(), claimed.getTargetId());
        ModerationJob job = mapper.lockJob(claimed.getJobId());
        if (job == null || !"PROCESSING".equals(job.getStatus()) || !Objects.equals(job.getClaimToken(), claimed.getClaimToken())) return;
        if (feed == null || feed.isDeleted() || target == null || target.isDeleted() || state == null
                || !Objects.equals(target.getUserNo(), job.getUserNo()) || !Objects.equals(target.getFeedId(), job.getFeedId())) {
            finish(job, "SKIPPED", null); return;
        }
        if ("REPLY".equals(job.getTargetType())) {
            ModerationTarget parent = mapper.lockTarget("COMMENT", target.getParentCommentId());
            if (parent == null || parent.isDeleted()) { finish(job, "SKIPPED", null); return; }
        }
        if (result.failed()) {
            boolean retry = result.retryable() && job.getAttemptCount() < 3;
            job.setStatus(retry ? "PENDING" : "FAILED");
            job.setRetryDelay(5 * job.getAttemptCount() * job.getAttemptCount());
            job.setLastErrorCode(result.errorCode()); job.setLastError(result.error());
            mapper.finishJob(job);
            return; // Infrastructure failures NEVER hide content or notify rejection.
        }
        if (Boolean.TRUE.equals(result.safe())) {
            if (job.getContentVersion() > state.getApprovedVersion()) {
                state.setApprovedVersion(job.getContentVersion()); state.setApprovedContent(job.getContent());
                state.setHasApproved(true); mapper.saveState(state);
            }
            finish(job, "APPROVED", null); return;
        }
        if (state.getLatestVersion() != job.getContentVersion()
                || !Objects.equals(target.getContent(), job.getContent())) {
            finish(job, "SUPERSEDED", null); return;
        }
        String action;
        if (state.isHasApproved()) {
            mapper.restoreContent(job.getTargetType(), job.getTargetId(), state.getApprovedContent());
            if ("FEED".equals(job.getTargetType())) feedService.restoreCaptionTags(job.getTargetId(), state.getApprovedContent());
            action = "RESTORED";
            if ("COMMENT".equals(job.getTargetType())) {
                commentMentions.restore(job.getUserNo(), job.getTargetId(), null, state.getApprovedContent());
                notifications.syncCommentMentions(job.getTargetId());
                notifications.refreshCommentSnippet(job.getTargetId());
            }
            if ("REPLY".equals(job.getTargetType())) {
                commentMentions.restore(job.getUserNo(), null, job.getTargetId(), state.getApprovedContent());
                notifications.syncReplyMentions(job.getTargetId(), false);
                notifications.refreshReplySnippet(job.getTargetId());
            }
        } else {
            mapper.hide(job.getTargetType(), job.getTargetId());
            action = "HIDDEN";
            switch (job.getTargetType()) {
                case "FEED" -> notifications.removeByFeed(job.getFeedId());
                case "COMMENT" -> {
                    feedMapper.adjustCommentCount(job.getFeedId(), -1);
                    notifications.removeCommentTree(job.getTargetId());
                }
                case "REPLY" -> notifications.removeReply(job.getTargetId());
                default -> throw new IllegalStateException("Unknown moderation target");
            }
        }
        if (!"FEED".equals(job.getTargetType())) notifications.syncFeedThread(job.getFeedId(), false);
        // Invalidate this visible revision without ever resurrecting a deleted row.
        state.setLatestVersion(state.getLatestVersion() + 1); mapper.saveState(state);
        finish(job, "REJECTED", action);
        String noun = switch (job.getTargetType()) { case "FEED" -> "게시글"; case "COMMENT" -> "댓글"; default -> "답글"; };
        String message = action.equals("RESTORED")
                ? noun + " 수정 내용이 부적절한 표현으로 판정되어 이전 내용으로 복구되었습니다."
                : noun + "에 부적절한 표현이 확인되어 숨김 처리되었습니다.";
        notifications.notifyModeration(job.getUserNo(), job.getJobId(), message);
        realtime.pushModeration(job, action);
        log.info("[AI moderation] applied jobId={} target={}:{} version={} action={}",
                job.getJobId(), job.getTargetType(), job.getTargetId(), job.getContentVersion(), action);
    }

    private void finish(ModerationJob job, String status, String action) {
        job.setStatus(status); job.setResultAction(action); mapper.finishJob(job);
    }

    @Transactional
    public void review(Long jobId, Long adminNo, String decision) {
        var snapshot = mapper.findJob(jobId);
        if (snapshot == null) throw new me._hanho.ultary.common.exception.BusinessException(me._hanho.ultary.common.exception.ErrorCode.NOT_FOUND);
        var feed = mapper.lockTarget("FEED", snapshot.getFeedId());
        var target = "FEED".equals(snapshot.getTargetType()) ? feed : mapper.lockTarget(snapshot.getTargetType(), snapshot.getTargetId());
        var state = mapper.lockState(snapshot.getTargetType(), snapshot.getTargetId());
        var job = mapper.lockJob(jobId);
        if (decision.equals(job.getAdminReviewStatus())) return;
        boolean undoDelete = "DELETED".equals(job.getAdminReviewStatus()) && "HIDDEN".equals(job.getResultAction());
        if (target == null || (target.isDeleted() && !undoDelete)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 대상 콘텐츠가 없거나 삭제되었습니다.");
        }
        if (!Objects.equals(target.getFeedId(), job.getFeedId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 기록과 현재 콘텐츠의 게시글 연결 정보가 일치하지 않습니다. 연결 정보를 확인해 주세요.");
        }
        if (feed == null || (feed.isDeleted() && !("FEED".equals(job.getTargetType()) && undoDelete))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 대상의 부모 게시글이 없거나 삭제되어 처리할 수 없습니다.");
        }
        if (state == null || !Objects.equals(target.getUserNo(), job.getUserNo())
                || !Objects.equals(state.getLatestVersion(), job.getContentVersion()) || !Objects.equals(target.getContent(), job.getContent())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 이후 콘텐츠가 수정되었거나 검사 정보가 일치하지 않습니다. 최신 오류 기록을 확인해 주세요.");
        }
        if ("REPLY".equals(job.getTargetType())) {
            var parent = mapper.lockTarget("COMMENT", target.getParentCommentId());
            if (parent == null || parent.isDeleted()) throw new BusinessException(ErrorCode.INVALID_INPUT, "답글의 부모 댓글이 없거나 삭제되어 처리할 수 없습니다.");
        }
        if (undoDelete && target.isDeleted()) {
            if (mapper.countOtherDeletedJobs(job.getTargetType(), job.getTargetId(), jobId) > 0
                    || mapper.countDeletedReports(job.getTargetType(), job.getTargetId()) > 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "다른 오류 기록이나 신고에서 삭제조치 중인 콘텐츠입니다. 해당 조치를 먼저 변경해 주세요.");
            }
            switch (job.getTargetType()) {
                case "FEED" -> feedService.restoreByAdmin(job.getTargetId());
                case "COMMENT" -> feedService.restoreCommentByAdmin(job.getTargetId(), job.getFeedId());
                case "REPLY" -> feedService.restoreReplyByAdmin(job.getTargetId(), job.getFeedId());
                default -> throw new IllegalStateException("Unknown moderation target");
            }
            realtime.pushModeration(job, "RESTORED");
        }
        if ("DELETED".equals(decision)) {
            mapper.hide(job.getTargetType(), job.getTargetId());
            switch (job.getTargetType()) {
                case "FEED" -> notifications.removeByFeed(job.getFeedId());
                case "COMMENT" -> { feedMapper.adjustCommentCount(job.getFeedId(), -1); notifications.removeCommentTree(job.getTargetId()); }
                case "REPLY" -> notifications.removeReply(job.getTargetId());
                default -> throw new IllegalStateException("Unknown moderation target");
            }
            if (!"FEED".equals(job.getTargetType())) notifications.syncFeedThread(job.getFeedId(), false);
            realtime.pushModeration(job, "HIDDEN");
        } else if ("KEPT".equals(decision)) {
            state.setApprovedVersion(job.getContentVersion()); state.setApprovedContent(job.getContent()); state.setHasApproved(true);
            mapper.saveState(state);
        }
        mapper.reviewJob(jobId, decision, adminNo);
        String noun = switch (job.getTargetType()) { case "FEED" -> "게시글"; case "COMMENT" -> "댓글"; default -> "답글"; };
        String message = switch (decision) {
            case "DELETED" -> "관리자 확인 후 " + noun + "이 숨김 처리되었습니다.";
            case "KEPT" -> undoDelete
                    ? "관리자 확인 후 " + noun + "의 삭제조치가 취소되어 복구되었습니다."
                    : "관리자 확인 결과 " + noun + "이 유지되었습니다.";
            default -> undoDelete
                    ? noun + "의 삭제조치가 취소되어 복구되었으며, 관리자 확인 대기 상태로 변경되었습니다."
                    : noun + "이 관리자 확인 대기 상태로 변경되었습니다.";
        };
        notifications.notifyModerationReview(job.getUserNo(), jobId, message);
    }
}
