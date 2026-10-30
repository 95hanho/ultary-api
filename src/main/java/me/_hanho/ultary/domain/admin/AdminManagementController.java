package me._hanho.ultary.domain.admin;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.admin.dto.response.*;

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
