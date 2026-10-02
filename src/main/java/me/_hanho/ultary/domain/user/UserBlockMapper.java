package me._hanho.ultary.domain.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.neighbor.model.BlockedUserRow;
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

	/** 내가 차단한 활성 유저. 최신 차단순 */
	List<BlockedUserRow> findBlockedUsers(@Param("blockerUserNo") Long blockerUserNo, @Param("limit") int limit);
}
