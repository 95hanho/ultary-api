package me._hanho.ultary.domain.neighbor;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.neighbor.model.Neighbor;
import me._hanho.ultary.domain.neighbor.model.NeighborListRow;

import java.util.List;

@Mapper
public interface NeighborMapper {

	/** 내가 요청해서 ACCEPTED된 수 (주민/팔로잉) */
	int countAcceptedAsRequester(@Param("userNo") Long userNo);

	/** 내가 받아서 ACCEPTED된 수 (이웃/팔로워) */
	int countAcceptedAsReceiver(@Param("userNo") Long userNo);

	int countAcceptedPair(
			@Param("userNoA") Long userNoA,
			@Param("userNoB") Long userNoB);

	Neighbor findByNeighborId(@Param("neighborId") Long neighborId);

	Neighbor findByPairKey(@Param("pairKey") String pairKey);

	int insert(Neighbor neighbor);

	int updateAccept(@Param("neighborId") Long neighborId);

	int updateReject(@Param("neighborId") Long neighborId);

	/** 거절 후 재요청 또는 역할 전환 재요청 */
	int updateReRequest(Neighbor neighbor);

	int deleteByNeighborId(@Param("neighborId") Long neighborId);

	List<NeighborListRow> findAcceptedResidents(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	List<NeighborListRow> findAcceptedNeighbors(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);
}
