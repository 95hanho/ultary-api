package me._hanho.ultary.domain.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.ai.model.AiRequestLog;

@Service
@RequiredArgsConstructor
public class AiLogService {
    private final AiRequestLogMapper mapper;
    /** Survives content rollback; also escapes read-only recommendation transactions. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AiRequestLog row) { mapper.insert(row); }
}
