package me._hanho.ultary.domain.main.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/** 최근 검색 한 건. userNo는 들어간 울타리 주인 */
@Getter
@Builder
public class SearchHistoryItemResponse {

	private Long userSearchHistoryId;
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	/** 그 울타리 주인의 펫 멘션. @ 없음. priority 순. 없으면 빈 배열 */
	private List<String> petTags;
	private LocalDateTime searchedAt;
}
