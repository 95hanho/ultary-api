package me._hanho.ultary.domain.story.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class StoryResponse {

	private Long storyId;
	private Long userNo;
	private String authorNickname;
	private Integer authorProfileFileId;
	private FileSummaryResponse authorProfileFile;
	private Long fileId;
	private FileSummaryResponse file;
	private String mediaType;
	private Long thumbnailFileId;
	private FileSummaryResponse thumbnailFile;
	private Integer durationSec;
	private String caption;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	/**
	 * 현재 조회자가 이 스토리를 봤는지 ({@code ultary_story_view}).
	 * 본인 스토리도 읽음 기록이 있을 때만 true. FE는 목록에서 첫 false부터 재생, 없으면 처음부터.
	 */
	private Boolean viewedByMe;
}
