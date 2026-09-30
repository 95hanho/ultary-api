package me._hanho.ultary.domain.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.PetTagHistoryRow;

@Mapper
public interface UserPetTagHistoryMapper {

	List<PetTagHistoryRow> findRecent(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("limit") int limit);

	int upsert(
			@Param("userNo") Long userNo,
			@Param("petId") Long petId);

	PetTagHistoryRow findByPair(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("petId") Long petId);

	int deleteByUserNo(@Param("userNo") Long userNo);
}
