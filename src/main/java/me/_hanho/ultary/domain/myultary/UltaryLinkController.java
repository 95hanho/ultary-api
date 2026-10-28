package me._hanho.ultary.domain.myultary;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

@RestController @RequiredArgsConstructor
public class UltaryLinkController {
    private final UltaryLinkService service;
    public record AddLink(@NotNull @Positive Long targetUserNo) {}
    @GetMapping("/api/v1/users/{userNo}/ultary-links")
    public ApiResponse<List<UltaryLinkService.Link>> list(@PathVariable Long userNo, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.list(userNo,principal == null ? null : principal.getUserNo()),"연결 울타리 조회 성공");
    }
    @GetMapping("/api/v1/my-ultary/links/search")
    public ApiResponse<List<UltaryLinkService.Link>> search(@RequestParam String q, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.search(principal.getUserNo(),q),"공개 울타리 검색 성공");
    }
    @PostMapping("/api/v1/my-ultary/links")
    public ApiResponse<Void> add(@Valid @RequestBody AddLink request, @AuthenticationPrincipal UserPrincipal principal) {
        service.add(principal.getUserNo(),request.targetUserNo());return ApiResponse.okEmpty("울타리 연결 성공");
    }
    @DeleteMapping("/api/v1/my-ultary/links/{targetUserNo}")
    public ApiResponse<Void> delete(@PathVariable Long targetUserNo, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(principal.getUserNo(),targetUserNo);return ApiResponse.okEmpty("울타리 연결 해제 성공");
    }
}
