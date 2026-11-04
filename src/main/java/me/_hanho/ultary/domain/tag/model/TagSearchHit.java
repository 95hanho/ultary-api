package me._hanho.ultary.domain.tag.model;

import lombok.Getter;
import lombok.Setter;

/** 태그명 검색 한 줄. feedCount는 조회자에게 보이는 게시글 수 */
@Getter
@Setter
public class TagSearchHit {

	private Long tagId;
	private String hashtag;
	/** 상업 설명 제목. 없으면 null. 동일 hashtag 구분용 */
	private String title;
	private Integer feedCount;
}
