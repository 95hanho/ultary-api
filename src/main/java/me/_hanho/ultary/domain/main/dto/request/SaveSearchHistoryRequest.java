package me._hanho.ultary.domain.main.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 최근 검색 저장. {@code targetUserNo} 또는 {@code tagId} 중 하나만.
 * 울타리 진입 → targetUserNo. 태그 피드 진입 → tagId.
 */
@Getter
@Setter
public class SaveSearchHistoryRequest {

	/** 들어간 울타리 주인 */
	private Long targetUserNo;

	/** 들어간 태그 피드 */
	private Long tagId;
}
