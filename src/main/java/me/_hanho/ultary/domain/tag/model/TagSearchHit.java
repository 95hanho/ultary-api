package me._hanho.ultary.domain.tag.model;

import lombok.Getter;
import lombok.Setter;

/** 태그명 검색 한 줄. feedCount는 조회자에게 보이는 게시글 수 */
@Getter
@Setter
public class TagSearchHit {

	private Long tagId;
	private String hashtag;
	private Integer feedCount;
}
