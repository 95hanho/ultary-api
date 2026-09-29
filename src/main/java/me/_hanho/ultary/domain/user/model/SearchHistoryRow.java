package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 최근 검색 목록 조인 행. 대상은 들어간 울타리 주인 */
@Getter
@Setter
public class SearchHistoryRow {

	private Long userSearchHistoryId;
	private Long targetUserNo;
	private String nickname;
	private Integer profileFileId;
	private LocalDateTime searchedAt;
}
