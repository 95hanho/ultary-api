package me._hanho.ultary.domain.dm.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/**
 * 메시지에 실린 공유.
 * FEED와 STORY 모두 실제 작성자 정보·스토리 상태를 포함한다.
 */
@Getter
@Builder
public class DmShareResponse {

	/** FEED | STORY */
	private String type;
	private boolean available;

	private Long feedId;
	private Long feedMediaId;
	/** 캐러셀 순서. 0이 첫 장 */
	private Integer mediaIndex;
	private Long authorUserNo;
	private String authorNickname;
	private FileSummaryResponse authorProfileFile;
	private boolean authorHasStory;
	private boolean authorHasUnviewed;
	private String content;

	private Long storyId;
	/** 공유한 그 사진. 영상이면 썸네일 */
	private FileSummaryResponse file;
}
