package me._hanho.ultary.domain.ai;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.ai.model.*;

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

    public ModerationTarget lock(String kind, Long id, Long feedId) {
        if (!"FEED".equals(kind)) mapper.lockTarget("FEED", feedId);
        return mapper.lockTarget(kind, id);
    }

    public void enqueue(String kind, Long id, Long feedId, Long userNo, String previous, String content, boolean created) {
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
