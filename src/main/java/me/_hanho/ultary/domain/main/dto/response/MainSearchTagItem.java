package me._hanho.ultary.domain.main.dto.response;

import lombok.Builder;
import lombok.Getter;

/** 검색창 태그명 한 줄. 클릭하면 그 태그의 게시글 그리드 */
@Getter
@Builder
public class MainSearchTagItem {

	private Long tagId;
	private String hashtag;
	/** 상업 설명 제목. 없으면 null. 동일 hashtag 구분용 */
	private String title;
	/** 이 조회자에게 보이는 게시글 수. 없으면 0 */
	private Integer feedCount;
}
