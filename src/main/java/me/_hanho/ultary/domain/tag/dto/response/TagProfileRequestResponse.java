package me._hanho.ultary.domain.tag.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class TagProfileRequestResponse {

	private Long requestId;
	private Long tagId;
	private String hashtag;
	private Long requesterUserNo;
	private String title;
	private String content;
	private String link;
	private List<Long> imageFileIds;
	private List<FileSummaryResponse> images;
	private String status;
	/** 거절일 때만. 승인·대기이면 null */
	private String rejectMessage;
	private LocalDateTime createdAt;
	private LocalDateTime reviewedAt;
}
