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
	/** 그 유저 펫의 mentionId. @ 없음. priority 순. 없으면 빈 배열 */
	private List<String> petTags;
	private String bio;
}
