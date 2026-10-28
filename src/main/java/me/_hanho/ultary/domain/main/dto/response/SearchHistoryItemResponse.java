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
	/** 현재 조회자가 볼 수 있는 스토리 유무·미열람 여부 */
	private boolean hasStory;
	private boolean hasUnviewed;
	/** type=USER: 펫 멘션(@ 없음). type=TAG면 [] */
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<String> petTags;
    private List<String> petNames;

	/** type=TAG: 태그. type=USER면 null */
	private Long tagId;
	private String hashtag;
	/** type=TAG: 조회자에게 보이는 태그 게시글 수. USER면 null */
	private Long feedCount;

	private LocalDateTime searchedAt;
}
