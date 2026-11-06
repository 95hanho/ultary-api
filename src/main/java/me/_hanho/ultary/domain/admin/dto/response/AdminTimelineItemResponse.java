package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/** 회원 상세 「주요 타임라인」 한 줄 */
@Getter
@Setter
public class AdminTimelineItemResponse {
	/** STORY_DELETED | FEED_DELETED | PASSWORD_CHANGED | NICKNAME_CHANGED | PET_DELETED | ACCOUNT_WITHDRAWN */
	private String type;
	/** 스토리·피드·펫 ID. 비밀번호·닉네임·탈퇴는 userNo */
	private Long id;
	private LocalDateTime occurredAt;
	/** 텍스트형 이벤트 문구. 미디어형은 null일 수 있음 */
	private String message;
	private List<String> petNames;
	/** 피드 본문 미리보기(최대 15자+…) */
	private String contentPreview;
	private List<FileSummaryResponse> files;
	/** RESTORE | RESET_PASSWORD | CLEAR_COOLDOWN | CANCEL_WITHDRAWAL */
	private String action;
	private String actionLabel;
}
