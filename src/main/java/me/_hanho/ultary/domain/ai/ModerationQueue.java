package me._hanho.ultary.domain.ai;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.ai.model.*;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

/** Only DB work here: the content write and enqueue always share one transaction. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ModerationQueue {
    private final ModerationMapper mapper;

    /** New posts were checked synchronously; preserve that baseline without a second job. */
    public void approveCreatedFeed(Long id, String content) {
        mapper.ensureState("FEED", id, content, true);
    }

    public void approveUpdatedFeed(Long id, String content) {
        mapper.ensureState("FEED", id, content, true);
        ModerationState state = mapper.lockState("FEED", id);
        state.setLatestVersion(state.getLatestVersion() + 1);
        state.setApprovedVersion(state.getLatestVersion());
        state.setApprovedContent(content);
        state.setHasApproved(true);
        mapper.saveState(state);
    }

    public ModerationTarget lock(String kind, Long id, Long feedId) {
        if (!"FEED".equals(kind)) mapper.lockTarget("FEED", feedId);
        return mapper.lockTarget(kind, id);
    }

    public void enqueue(String kind, Long id, Long feedId, Long userNo, String previous, String content, boolean created) {
        ModerationTarget target = lock(kind, id, feedId);
        if (target == null || target.isDeleted()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 대상이 없거나 삭제되었습니다.");
        }
        if (!Objects.equals(target.getFeedId(), feedId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 대상의 게시글 연결 정보가 일치하지 않습니다.");
        }
        if (!Objects.equals(target.getUserNo(), userNo) || !Objects.equals(target.getContent(), content)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검사 대상의 작성자 또는 본문이 일치하지 않습니다.");
        }
        if (!created && Objects.equals(previous, content)) return;
        mapper.ensureState(kind, id, previous, !created);
        ModerationState state = mapper.lockState(kind, id);
        state.setLatestVersion(state.getLatestVersion() + 1);
        mapper.saveState(state);
        ModerationJob job = new ModerationJob();
        job.setTargetType(kind); job.setTargetId(id); job.setFeedId(feedId); job.setUserNo(userNo);
        job.setContentVersion(state.getLatestVersion()); job.setOperation(created ? "CREATE" : "UPDATE"); job.setContent(content);
        mapper.insertJob(job);
    }
}
