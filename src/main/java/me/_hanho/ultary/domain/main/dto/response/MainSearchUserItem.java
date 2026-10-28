package me._hanho.ultary.domain.main.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class MainSearchUserItem {

	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	/** 현재 조회자가 볼 수 있는 스토리 유무·미열람 여부 */
	private boolean hasStory;
	private boolean hasUnviewed;
	/** 그 유저 펫의 mentionId. @ 없음. priority 순. 없으면 빈 배열 */
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<String> petTags;
    private List<String> petNames;
	private String bio;
}
