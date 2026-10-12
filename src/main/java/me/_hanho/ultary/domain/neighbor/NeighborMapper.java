package me._hanho.ultary.domain.neighbor;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.neighbor.model.Neighbor;
import me._hanho.ultary.domain.neighbor.model.NeighborListRow;

@Mapper
public interface NeighborMapper {

	/** 내가 팔로우하는 수. 맞팔 포함 */
	int countAcceptedAsRequester(@Param("userNo") Long userNo);

	/** 나를 팔로우하는 수 (팔로워). 맞팔 포함 */
	int countAcceptedAsReceiver(@Param("userNo") Long userNo);

	int countAcceptedPair(
			@Param("userNoA") Long userNoA,
			@Param("userNoB") Long userNoB);

	/** 조회자가 그 계정을 팔로우. 맞팔 포함. 상대만 팔로우한 경우는 0 */
	int countViewerFollows(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("ownerUserNo") Long ownerUserNo);

	Neighbor findByNeighborId(@Param("neighborId") Long neighborId);

	Neighbor findByPairKey(@Param("pairKey") String pairKey);

	int insert(Neighbor neighbor);

	int updateAccept(@Param("neighborId") Long neighborId);

	int updateReject(@Param("neighborId") Long neighborId);

	/** 거절 후 재요청 또는 역할 전환 재요청 */
	int updateReRequest(Neighbor neighbor);

	/** 수신자가 요청자를 맞팔. ACCEPTED 이고 아직 맞팔이 아닐 때만 */
	int updateFollowBack(@Param("neighborId") Long neighborId, @Param("userNo") Long userNo);

	/** 맞팔에서 요청자의 팔로우만 끊고, 수신자→요청자 방향을 정방향으로 남긴다 */
	int keepIncomingFollow(
			@Param("neighborId") Long neighborId,
			@Param("requesterUserNo") Long requesterUserNo,
			@Param("receiverUserNo") Long receiverUserNo);

	/** 맞팔에서 수신자의 팔로우만 끊는다 */
	int clearFollowBack(@Param("neighborId") Long neighborId);

	int deleteByNeighborId(@Param("neighborId") Long neighborId);

	/** viewer가 팔로우 중인 userNos */
	List<Long> findFollowedUserNos(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("userNos") Collection<Long> userNos);

	/** userNos 중에서 viewer를 팔로우 중인 사람. ACCEPTED. 맞팔 포함 */
	List<Long> findFollowerUserNos(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("userNos") Collection<Long> userNos);

	List<NeighborListRow> findAcceptedResidents(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);

	List<NeighborListRow> findAcceptedNeighbors(
			@Param("userNo") Long userNo,
			@Param("limit") int limit);
}
