package me._hanho.ultary.domain.test;

import java.time.LocalDateTime;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.story.StoryMapper;
import me._hanho.ultary.domain.test.dto.response.NicknameCooldownResetResponse;
import me._hanho.ultary.domain.test.dto.response.StoryViewResetResponse;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 운영 체험 도구. 대상 회원은 요청 입력이 아니라 인증된 본인으로 한정한다. */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Transactional
public class ExperienceController {
    private final StoryMapper storyMapper;
    private final UserMapper userMapper;

    @DeleteMapping("/story-views")
    public ApiResponse<StoryViewResetResponse> resetStoryViews(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        int deleted = storyMapper.deleteViewsByViewerUserNo(principal.getUserNo());
        return ApiResponse.ok(StoryViewResetResponse.builder()
                .viewerUserNo(principal.getUserNo()).deletedCount(deleted).build(), "스토리 읽음 초기화 성공");
    }

    @PostMapping("/nickname-cooldown")
    public ApiResponse<NicknameCooldownResetResponse> resetNicknameCooldown(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        LocalDateTime changedAt = LocalDateTime.now().minusDays(31);
        if (userMapper.updateNicknameChangedAt(principal.getUserNo(), changedAt) == 0)
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return ApiResponse.ok(NicknameCooldownResetResponse.builder()
                .userNo(principal.getUserNo()).nicknameChangedAt(changedAt).build(), "닉네임태그 변경 기간 초기화 성공");
    }
}
