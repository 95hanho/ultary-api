package me._hanho.ultary.domain.myultary.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/** 그리드용 요약 (첫 미디어 썸네일) */
@Getter
@Builder
public class FeedGridItemResponse {

	private Long feedId;
	private Long coverFileId;
	private Long coverThumbnailFileId;
	private String coverMediaType;
	private Integer mediaCount;
	private Integer likeCount;
	private Integer commentCount;
	private LocalDateTime createdAt;
}
