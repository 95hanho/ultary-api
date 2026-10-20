package me._hanho.ultary.domain.ai;

import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ModerationWorker {
    private final ModerationMapper mapper;
    private final ModerationTransactions transactions;
    private final AiService ai;
    private final ThreadPoolTaskExecutor executor;
    private final boolean enabled;

    public ModerationWorker(ModerationMapper mapper, ModerationTransactions transactions, AiService ai,
            @Qualifier("moderationExecutor") ThreadPoolTaskExecutor executor,
            @Value("${ai.moderation.worker-enabled:true}") boolean enabled) {
        this.mapper = mapper; this.transactions = transactions; this.ai = ai; this.executor = executor; this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${ai.moderation.poll-ms:1000}", initialDelayString = "${ai.moderation.initial-delay-ms:5000}")
    public void poll() {
        if (!enabled || executor.getActiveCount() >= 2) return;
        try {
            for (Long id : mapper.findReady(2)) {
                try { executor.execute(() -> run(id)); }
                catch (RejectedExecutionException busy) { break; } // Remains durable in DB.
            }
        } catch (Exception ex) {
            log.error("[AI moderation] polling unavailable cause={}", ex.getClass().getSimpleName());
        }
    }

    void run(Long id) {
        try {
            var job = transactions.claim(id);
            if (job != null) transactions.complete(job, ai.moderate(job));
        } catch (Exception ex) {
            // Unfinished claims are recoverable after their DB lease expires (120 seconds).
            log.error("[AI moderation] jobId={} unfinished; lease recovery will retry. cause={}", id, ex.getClass().getSimpleName());
        }
    }
}
