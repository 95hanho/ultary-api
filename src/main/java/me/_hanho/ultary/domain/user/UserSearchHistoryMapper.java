package me._hanho.ultary.domain.user;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.SearchHistoryRow;
import me._hanho.ultary.domain.user.model.UserSearchHistory;

@Mapper
public interface UserSearchHistoryMapper {

	UserSearchHistory findOwnById(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("userSearchHistoryId") Long userSearchHistoryId);

	List<SearchHistoryRow> findRecent(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("cursorSearchedAt") LocalDateTime cursorSearchedAt,
			@Param("cursorHistoryId") Long cursorHistoryId,
			@Param("limit") int limit);

	int upsert(
			@Param("userNo") Long userNo,
			@Param("targetUserNo") Long targetUserNo);

	SearchHistoryRow findByPair(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("targetUserNo") Long targetUserNo);

	int deleteByUserNo(@Param("userNo") Long userNo);
}
