package me._hanho.ultary.domain.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.request.AdminTimelineActionRequest;
import me._hanho.ultary.domain.admin.dto.response.*;
import me._hanho.ultary.security.principal.AdminPrincipal;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminManagementController {
    private final AdminManagementService service;
    private final me._hanho.ultary.domain.file.FileService files;

    @GetMapping("/users")
    public ApiResponse<AdminPageResponse<AdminUserResponse>> users(@RequestParam(required=false) String q,
            @RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(service.users(q,status,page,size));
    }
    @GetMapping("/users/{userNo}")
    public ApiResponse<AdminUserDetailResponse> user(@PathVariable Long userNo) { return ApiResponse.ok(service.user(userNo)); }

    @GetMapping("/users/{userNo}/timeline")
    public ApiResponse<AdminTimelineResponse> timeline(@PathVariable Long userNo) {
        return ApiResponse.ok(service.timeline(userNo), "주요 타임라인 조회 성공");
    }

    @PostMapping("/users/{userNo}/timeline/actions")
    public ApiResponse<AdminTimelineActionResponse> timelineAction(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long userNo,
            @Valid @RequestBody AdminTimelineActionRequest request) {
        String type = request.getType() == null ? "" : request.getType().trim();
        Long id = request.getId();
        String reason = request.getReason();
        if (id == null) throw new BusinessException(ErrorCode.INVALID_INPUT);

        return switch (type) {
            case "STORY_DELETED" -> {
                service.execute(principal.getAdminNo(), "STORY_RESTORED", "STORY", id, null, reason,
                        () -> service.restoreDeletedStory(userNo, id));
                yield ApiResponse.ok(AdminTimelineActionResponse.builder().type(type).id(id).build(),
                        "스토리 삭제 취소 성공");
            }
            case "FEED_DELETED" -> {
                service.execute(principal.getAdminNo(), "FEED_RESTORED", "FEED", id, null, reason,
                        () -> service.restoreDeletedFeed(userNo, id));
                yield ApiResponse.ok(AdminTimelineActionResponse.builder().type(type).id(id).build(),
                        "게시글 삭제 취소 성공");
            }
            case "PET_DELETED" -> {
                service.execute(principal.getAdminNo(), "PET_RESTORED", "PET", id, null, reason,
                        () -> service.restoreDeletedPet(userNo, id));
                yield ApiResponse.ok(AdminTimelineActionResponse.builder().type(type).id(id).build(),
                        "반려동물 삭제 취소 성공");
            }
            case "PASSWORD_CHANGED" -> {
                if (!id.equals(userNo)) throw new BusinessException(ErrorCode.INVALID_INPUT);
                String temporary = service.execute(principal.getAdminNo(), "PASSWORD_RESET", "USER", userNo, null, reason,
                        () -> service.resetPassword(userNo));
                yield ApiResponse.ok(
                        AdminTimelineActionResponse.builder().type(type).id(userNo).temporaryPassword(temporary).build(),
                        "비밀번호 초기화 성공");
            }
            case "NICKNAME_CHANGED" -> {
                if (!id.equals(userNo)) throw new BusinessException(ErrorCode.INVALID_INPUT);
                service.execute(principal.getAdminNo(), "NICKNAME_COOLDOWN_CLEARED", "USER", userNo, null, reason,
                        () -> service.clearNicknameCooldown(userNo));
                yield ApiResponse.ok(AdminTimelineActionResponse.builder().type(type).id(userNo).build(),
                        "닉네임태그 변경 제한 초기화 성공");
            }
            case "ACCOUNT_WITHDRAWN" -> {
                if (!id.equals(userNo)) throw new BusinessException(ErrorCode.INVALID_INPUT);
                service.execute(principal.getAdminNo(), "WITHDRAWAL_CANCELLED", "USER", userNo, null, reason,
                        () -> service.cancelWithdrawal(userNo));
                yield ApiResponse.ok(AdminTimelineActionResponse.builder().type(type).id(userNo).build(),
                        "탈퇴 취소 성공");
            }
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    @GetMapping("/users/{userNo}/files/{fileId}/content")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> profileFile(@PathVariable Long userNo,@PathVariable Long fileId) {
        service.assertProfileFile(userNo,fileId);
        var meta=files.requireActive(fileId);
        org.springframework.http.MediaType type=org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        if(meta.getMimeType()!=null)try{type=org.springframework.http.MediaType.parseMediaType(meta.getMimeType());}catch(IllegalArgumentException ignored){}
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control","private, no-store").contentType(type).body(files.getContent(fileId));
    }
    @GetMapping("/actions")
    public ApiResponse<AdminPageResponse<AdminActionLogResponse>> actions(@RequestParam(required=false) String q,
            @RequestParam(required=false) String targetType,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(service.actions(q,targetType,page,size));
    }
}
