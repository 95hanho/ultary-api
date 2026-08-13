package me._hanho.ultary.domain.story.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryResponse {

	private Long storyId;
	private Long userNo;
	private String authorNickname;
	private Integer authorProfileFileId;
	private Long fileId;
	private String mediaType;
	private Long thumbnailFileId;
	private Integer durationSec;
	private String caption;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	/** 현재 조회자가 이미 본 스토리인지 */
	private Boolean viewedByMe;
}
