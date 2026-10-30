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
    private final AdminManagementService management;
    private final AdminAiLogPreviewService previews;
    private final me._hanho.ultary.domain.file.FileService files;

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
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(service.list(status, featureType, targetType, targetId, errorCode,
                userNo, moderationJobId, cursorAiRequestId, limit, phone, email), "AI 요청 로그 조회 성공");
    }

    @GetMapping("/{aiRequestId}")
    public ApiResponse<AdminAiLogResponse> detail(@PathVariable Long aiRequestId) {
        return ApiResponse.ok(service.detail(aiRequestId), "AI 요청 로그 상세 조회 성공");
    }

    public record ReviewRequest(String status) {}

    @GetMapping("/{aiRequestId}/preview")
    public ApiResponse<me._hanho.ultary.domain.report.dto.response.ReportFeedPreviewResponse> preview(@PathVariable Long aiRequestId) {
        return ApiResponse.ok(previews.preview(aiRequestId));
    }

    @GetMapping("/{aiRequestId}/files/{fileId}/content")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> previewFile(
            @PathVariable Long aiRequestId, @PathVariable Long fileId) {
        previews.assertPreviewFile(aiRequestId, fileId);
        var file = files.requireActive(fileId);
        org.springframework.http.MediaType type = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        if (file.getMimeType() != null) {
            try { type = org.springframework.http.MediaType.parseMediaType(file.getMimeType()); }
            catch (IllegalArgumentException ignored) { }
        }
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .contentType(type).body(files.getContent(fileId));
    }
    @PostMapping("/{aiRequestId}/review")
    public ApiResponse<AdminAiLogResponse> review(@PathVariable Long aiRequestId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal me._hanho.ultary.security.principal.AdminPrincipal principal,
            @RequestBody ReviewRequest request) {
        return ApiResponse.ok(management.execute(principal.getAdminNo(), "AI_REVIEW", "AI_REVIEW", aiRequestId, null, null,
                () -> service.review(aiRequestId, principal.getAdminNo(), request.status())));
    }
}
