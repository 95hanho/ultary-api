package me._hanho.ultary.domain.dm.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/**
 * 메시지에 실린 공유.
 * FEED는 작성자 프로필·닉네임·그 사진·본문. STORY는 사진만.
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
	private String content;

	private Long storyId;
	/** 공유한 그 사진. 영상이면 썸네일 */
	private FileSummaryResponse file;
}
