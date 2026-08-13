package me._hanho.ultary.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.UserBlock;

@Mapper
public interface UserBlockMapper {

	UserBlock findActiveByPair(
			@Param("blockerUserNo") Long blockerUserNo,
			@Param("blockedUserNo") Long blockedUserNo);

	/** 양방향 중 하나라도 활성 차단이면 반환 (우선 blocker=a) */
	UserBlock findActiveEitherWay(
			@Param("userNoA") Long userNoA,
			@Param("userNoB") Long userNoB);

	UserBlock findAnyByPair(
			@Param("blockerUserNo") Long blockerUserNo,
			@Param("blockedUserNo") Long blockedUserNo);

	int insert(UserBlock block);

	int restore(@Param("userBlockId") Long userBlockId);

	int softDelete(
			@Param("blockerUserNo") Long blockerUserNo,
			@Param("blockedUserNo") Long blockedUserNo);
}
