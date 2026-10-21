package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 최근 검색 목록 조인 행 */
@Getter
@Setter
public class SearchHistoryRow {

	private Long userSearchHistoryId;
	/** USER | TAG */
	private String historyType;
	private Long targetUserNo;
	private String nickname;
	private Integer profileFileId;
	private Long targetTagId;
	private String hashtag;
	private Long feedCount;
	private LocalDateTime searchedAt;
}
