package me._hanho.ultary.domain.admin;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiRequestLogMapper;
import me._hanho.ultary.domain.admin.dto.response.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiLogService {
    private final AiRequestLogMapper mapper;

    public int countFailed() { return mapper.countFailed(); }

    public AdminAiLogPageResponse list(String status, String featureType, String targetType, Long targetId,
            String errorCode, Long userNo, Long moderationJobId, Long cursorAiRequestId, int limit) {
        status = option(status, Set.of("ALL", "PENDING", "SUCCESS", "FAILED"), "status");
        featureType = option(featureType, Set.of("FEED_CAPTION", "HASHTAG", "PET_PROFILE", "COMMENT_FILTER", "ALT_TEXT"), "featureType");
        targetType = option(targetType, Set.of("FEED", "COMMENT", "REPLY"), "targetType");
        if (limit < 1 || limit > 100 || invalidId(userNo) || invalidId(targetId) || invalidId(moderationJobId) || invalidId(cursorAiRequestId)
                || (errorCode != null && !errorCode.matches("[A-Z0-9_]{1,64}"))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        var rows = mapper.listForAdmin("ALL".equals(status) ? null : status, featureType, targetType, targetId,
                errorCode, userNo, moderationJobId, cursorAiRequestId, limit + 1);
        boolean hasMore = rows.size() > limit;
        var items = hasMore ? rows.subList(0, limit) : rows;
        int totalCount = mapper.countForAdmin("ALL".equals(status) ? null : status, featureType, targetType,
                targetId, errorCode, userNo, moderationJobId);
        return new AdminAiLogPageResponse(items, hasMore ? items.get(items.size() - 1).getAiRequestId() : null, totalCount);
    }

    public AdminAiLogResponse detail(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
        var row = mapper.findForAdmin(id);
        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return row;
    }

    private boolean invalidId(Long value) { return value != null && value <= 0; }

    private String option(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new BusinessException(ErrorCode.INVALID_INPUT, field + " 값이 올바르지 않습니다.");
        return normalized;
    }
}
