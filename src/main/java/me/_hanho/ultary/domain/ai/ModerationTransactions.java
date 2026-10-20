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

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationTransactions {
    private final ModerationMapper mapper;
    private final FeedMapper feedMapper;
    private final FeedService feedService;
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
                || !Objects.equals(target.getUserNo(), job.getUserNo())) {
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
            if ("COMMENT".equals(job.getTargetType())) notifications.refreshCommentSnippet(job.getTargetId());
            if ("REPLY".equals(job.getTargetType())) notifications.refreshReplySnippet(job.getTargetId());
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
        realtime.pushModeration(job, action, message);
        log.info("[AI moderation] applied jobId={} target={}:{} version={} action={}",
                job.getJobId(), job.getTargetType(), job.getTargetId(), job.getContentVersion(), action);
    }

    private void finish(ModerationJob job, String status, String action) {
        job.setStatus(status); job.setResultAction(action); mapper.finishJob(job);
    }
}
