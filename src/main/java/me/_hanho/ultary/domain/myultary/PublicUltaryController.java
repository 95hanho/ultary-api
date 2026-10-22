package me._hanho.ultary.domain.myultary;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.myultary.dto.response.PublicUltaryResponse;

@RestController
@RequiredArgsConstructor
public class PublicUltaryController {
    private final MyUltaryService service;

    @GetMapping("/api/v1/public/ultaries/{nickname}")
    public ResponseEntity<ApiResponse<PublicUltaryResponse>> get(@PathVariable String nickname) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiResponse.ok(service.getPublicProfile(nickname), "공개 울타리 조회 성공"));
    }
}
