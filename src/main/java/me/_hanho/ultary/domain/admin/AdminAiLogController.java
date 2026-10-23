package me._hanho.ultary.domain.admin;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.response.*;

/** Protected by the existing admin JWT authentication path. */
@RestController
@RequestMapping("/api/v1/admin/ai/logs")
@RequiredArgsConstructor
public class AdminAiLogController {
    private final AdminAiLogService service;

    @GetMapping
    public ApiResponse<AdminAiLogPageResponse> list(
            @RequestParam(defaultValue = "FAILED") String status,
            @RequestParam(required = false) String featureType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) Long userNo,
            @RequestParam(required = false) Long moderationJobId,
            @RequestParam(required = false) Long cursorAiRequestId,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(service.list(status, featureType, targetType, targetId, errorCode,
                userNo, moderationJobId, cursorAiRequestId, limit), "AI 요청 로그 조회 성공");
    }

    @GetMapping("/{aiRequestId}")
    public ApiResponse<AdminAiLogResponse> detail(@PathVariable Long aiRequestId) {
        return ApiResponse.ok(service.detail(aiRequestId), "AI 요청 로그 상세 조회 성공");
    }

    public record ReviewRequest(String status) {}
    @PostMapping("/{aiRequestId}/review")
    public ApiResponse<AdminAiLogResponse> review(@PathVariable Long aiRequestId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal me._hanho.ultary.security.principal.AdminPrincipal principal,
            @RequestBody ReviewRequest request) {
        return ApiResponse.ok(service.review(aiRequestId, principal.getAdminNo(), request.status()));
    }
}
