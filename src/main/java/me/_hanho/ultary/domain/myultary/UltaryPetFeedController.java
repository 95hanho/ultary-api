package me._hanho.ultary.domain.myultary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

@RestController
@RequiredArgsConstructor
public class UltaryPetFeedController {
    private final MyUltaryService myUltaryService;
    @GetMapping("/api/v1/users/{userNo}/pet-feeds")
    public ApiResponse<List<me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse>> petFeeds(
            @PathVariable Long userNo, @RequestParam Long petId,
            @RequestParam(defaultValue = "feed") String tab, @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(myUltaryService.getPetFeeds(principal, userNo, petId, tab, limit), "펫 게시글 조회 성공");
    }
}
