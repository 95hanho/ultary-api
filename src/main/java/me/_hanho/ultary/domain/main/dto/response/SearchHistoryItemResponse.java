package me._hanho.ultary.domain.main.dto.response;

import java.time.LocalDateTime;

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
	private LocalDateTime searchedAt;
}
