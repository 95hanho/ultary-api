package me._hanho.ultary.domain.myultary.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/** 그리드용 요약 (첫 미디어 커버) */
@Getter
@Builder
public class FeedGridItemResponse {

	private Long feedId;
	private Long coverFileId;
	private FileSummaryResponse coverFile;
	private Long coverThumbnailFileId;
	private FileSummaryResponse coverThumbnailFile;
	private String coverMediaType;
	private Integer mediaCount;
	private Integer likeCount;
	private Integer commentCount;
	private LocalDateTime createdAt;
}
