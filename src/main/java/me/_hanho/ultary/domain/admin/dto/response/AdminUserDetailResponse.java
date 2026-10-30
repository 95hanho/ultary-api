package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;

public record AdminUserDetailResponse(AdminUserResponse user, List<PetResponse> pets,
        List<FeedItem> feeds, List<ReportItem> reports) {
    public record FeedItem(Long feedId, String content, String visibility, boolean deleted, LocalDateTime createdAt) {}
    public record ReportItem(Long reportId, String targetType, String status, String reason, LocalDateTime createdAt) {}
}
