package me._hanho.ultary.domain.main.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/** 최근 검색 한 건. type=USER(울타리) | TAG(해시태그 피드) */
@Getter
@Builder
public class SearchHistoryItemResponse {

	private Long userSearchHistoryId;
	/** USER | TAG */
	private String type;

	/** type=USER: 울타리 주인. type=TAG면 null */
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	/** type=USER: 펫 멘션(@ 없음). type=TAG면 [] */
	private List<String> petTags;

	/** type=TAG: 태그. type=USER면 null */
	private Long tagId;
	private String hashtag;

	private LocalDateTime searchedAt;
}
