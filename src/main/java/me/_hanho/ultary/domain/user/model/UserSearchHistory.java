package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user_search_history — 최근 검색 (NICKNAME / PET / TAG) */
@Getter
@Setter
public class UserSearchHistory {

	private Long userSearchHistoryId;
	private Long userNo;
	/** NICKNAME | PET | TAG */
	private String searchType;
	private Long targetUserNo;
	private Long targetPetId;
	private Long targetTagId;
	private String keyword;
	private LocalDateTime searchedAt;
}
