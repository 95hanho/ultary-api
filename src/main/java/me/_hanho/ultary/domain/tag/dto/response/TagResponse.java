package me._hanho.ultary.domain.tag.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class TagResponse {

	private Long tagId;
	private String hashtag;
	/** USER 또는 COMMERCIAL */
	private String purpose;
	/** NONE, PENDING, APPROVED, REJECTED. 설명 필드는 APPROVED일 때만 채워진다 */
	private String profileStatus;
	private String title;
	private String handle;
	private LocalDateTime handleChangedAt;
	private String content;
	private String link;
	private Integer useCount;
	/** 이 태그가 달린 삭제되지 않은 게시글 수. 상세 조회에서만 채운다. 지금 보는 글도 포함 */
	private Integer feedCount;
	private Long createdByUserNo;
	private List<Long> imageFileIds;
	private List<FileSummaryResponse> images;
	/** 이 조회자가 넣은 최신 설명 요청. 다른 사람에게는 null */
	private TagProfileRequestResponse profileRequest;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
