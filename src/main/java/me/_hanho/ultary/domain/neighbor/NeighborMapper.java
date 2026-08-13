package me._hanho.ultary.domain.neighbor;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NeighborMapper {

	/** 내가 요청해서 ACCEPTED된 수 (주민/팔로잉) */
	int countAcceptedAsRequester(@Param("userNo") Long userNo);

	/** 내가 받아서 ACCEPTED된 수 (이웃/팔로워) */
	int countAcceptedAsReceiver(@Param("userNo") Long userNo);
}
