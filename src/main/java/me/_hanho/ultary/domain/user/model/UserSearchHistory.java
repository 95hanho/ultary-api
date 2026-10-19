package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user_search_history — 들어간 유저 울타리 또는 태그 피드 */
@Getter
@Setter
public class UserSearchHistory {

	private Long userSearchHistoryId;
	private Long userNo;
	private Long targetUserNo;
	private Long targetTagId;
	private LocalDateTime searchedAt;
}
