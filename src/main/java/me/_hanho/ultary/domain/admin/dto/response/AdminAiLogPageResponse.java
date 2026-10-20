package me._hanho.ultary.domain.admin.dto.response;

import java.util.List;

public record AdminAiLogPageResponse(List<AdminAiLogResponse> items, Long nextCursorAiRequestId, int totalCount) {}
