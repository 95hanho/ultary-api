package me._hanho.ultary.domain.neighbor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborListItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborRelationResponse;
import me._hanho.ultary.domain.neighbor.dto.response.UserUltaryResponse;
import me._hanho.ultary.domain.neighbor.model.Neighbor;
import me._hanho.ultary.domain.neighbor.model.NeighborListRow;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.domain.user.model.UserBlock;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NeighborService {

	private static final int DEFAULT_LIMIT = 30;
	private static final int MAX_LIMIT = 50;

	private final NeighborMapper neighborMapper;
	private final UserBlockMapper userBlockMapper;
	private final UserMapper userMapper;
	private final PetMapper petMapper;
	private final FeedMapper feedMapper;
	private final StoryService storyService;

	@Transactional(readOnly = true)
	public UserUltaryResponse getUltary(UserPrincipal principal, Long targetUserNo) {
		User target = requireActiveUser(targetUserNo);
		Long me = principal.getUserNo();

		boolean blockedByMe = userBlockMapper.findActiveByPair(me, targetUserNo) != null;
		boolean blockedMe = userBlockMapper.findActiveByPair(targetUserNo, me) != null;
		if (blockedMe) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}

		Neighbor neighbor = neighborMapper.findByPairKey(pairKey(me, targetUserNo));
		String relationStatus = resolveRelationStatus(me, neighbor, blockedByMe);
		Long neighborId = neighbor == null ? null : neighbor.getNeighborId();

		return UserUltaryResponse.builder()
				.userNo(target.getUserNo())
				.nickname(target.getNickname())
				.defaultNickname(Boolean.TRUE.equals(target.getIsDefaultNickname()))
				.profileFileId(target.getProfileFileId())
				.bio(target.getBio())
				.regionSido(target.getRegionSido())
				.regionSigungu(target.getRegionSigungu())
				.hasStory(storyService.hasActiveStory(targetUserNo))
				.residentCount(neighborMapper.countAcceptedAsRequester(targetUserNo))
				.neighborCount(neighborMapper.countAcceptedAsReceiver(targetUserNo))
				.petCount(petMapper.countActiveByUserNo(targetUserNo))
				.feedCount(feedMapper.countActiveByUserNo(targetUserNo))
				.relationStatus(relationStatus)
				.neighborId(neighborId)
				.blockedByMe(blockedByMe)
				.blockedMe(false)
				.build();
	}

	@Transactional(readOnly = true)
	public List<NeighborListItemResponse> listNeighbors(
			UserPrincipal principal, Long targetUserNo, String type, Integer limit) {
		requireActiveUser(targetUserNo);
		Long me = principal.getUserNo();
		if (userBlockMapper.findActiveByPair(targetUserNo, me) != null) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}

		int resolvedLimit = resolveLimit(limit);
		String listType = normalizeListType(type);
		List<NeighborListRow> rows = "NEIGHBORS".equals(listType)
				? neighborMapper.findAcceptedNeighbors(targetUserNo, resolvedLimit)
				: neighborMapper.findAcceptedResidents(targetUserNo, resolvedLimit);

		List<NeighborListItemResponse> result = new ArrayList<>();
		for (NeighborListRow row : rows) {
			result.add(NeighborListItemResponse.builder()
					.neighborId(row.getNeighborId())
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.profileFileId(row.getProfileFileId())
					.status(row.getStatus())
					.listType(row.getListType())
					.hasStory(storyService.hasActiveStory(row.getUserNo()))
					.build());
		}
		return result;
	}

	@Transactional
	public NeighborRelationResponse request(UserPrincipal principal, Long targetUserNo) {
		Long me = principal.getUserNo();
		if (me.equals(targetUserNo)) {
			throw new BusinessException(ErrorCode.NEIGHBOR_SELF);
		}
		requireActiveUser(targetUserNo);
		assertNotBlockedEitherWay(me, targetUserNo);

		String key = pairKey(me, targetUserNo);
		Neighbor existing = neighborMapper.findByPairKey(key);
		if (existing == null) {
			Neighbor created = new Neighbor();
			created.setRequesterUserNo(me);
			created.setReceiverUserNo(targetUserNo);
			created.setPairKey(key);
			created.setStatus("PENDING");
			neighborMapper.insert(created);
			log.info("[request] me={} target={} neighborId={}", me, targetUserNo, created.getNeighborId());
			return toRelation(created);
		}

		if ("ACCEPTED".equals(existing.getStatus())) {
			throw new BusinessException(ErrorCode.NEIGHBOR_ALREADY_EXISTS);
		}
		if ("PENDING".equals(existing.getStatus())) {
			if (me.equals(existing.getRequesterUserNo())) {
				throw new BusinessException(ErrorCode.NEIGHBOR_ALREADY_REQUESTED);
			}
			throw new BusinessException(ErrorCode.NEIGHBOR_PENDING_RECEIVED);
		}
		if ("BLOCKED".equals(existing.getStatus())) {
			throw new BusinessException(ErrorCode.NEIGHBOR_BLOCKED);
		}
		// REJECTED → 재요청 (요청자/수신자 갱신)
		existing.setRequesterUserNo(me);
		existing.setReceiverUserNo(targetUserNo);
		existing.setStatus("PENDING");
		neighborMapper.updateReRequest(existing);
		log.info("[request:re] me={} target={} neighborId={}", me, targetUserNo, existing.getNeighborId());
		return toRelation(neighborMapper.findByNeighborId(existing.getNeighborId()));
	}

	@Transactional
	public NeighborRelationResponse accept(UserPrincipal principal, Long neighborId) {
		Neighbor neighbor = requireNeighbor(neighborId);
		if (!principal.getUserNo().equals(neighbor.getReceiverUserNo())) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (!"PENDING".equals(neighbor.getStatus())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "대기 중인 요청만 수락할 수 있습니다.");
		}
		assertNotBlockedEitherWay(neighbor.getRequesterUserNo(), neighbor.getReceiverUserNo());
		int updated = neighborMapper.updateAccept(neighborId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.NEIGHBOR_NOT_FOUND);
		}
		log.info("[accept] neighborId={} receiver={}", neighborId, principal.getUserNo());
		return toRelation(neighborMapper.findByNeighborId(neighborId));
	}

	@Transactional
	public NeighborRelationResponse reject(UserPrincipal principal, Long neighborId) {
		Neighbor neighbor = requireNeighbor(neighborId);
		if (!principal.getUserNo().equals(neighbor.getReceiverUserNo())) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (!"PENDING".equals(neighbor.getStatus())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "대기 중인 요청만 거절할 수 있습니다.");
		}
		int updated = neighborMapper.updateReject(neighborId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.NEIGHBOR_NOT_FOUND);
		}
		log.info("[reject] neighborId={} receiver={}", neighborId, principal.getUserNo());
		return toRelation(neighborMapper.findByNeighborId(neighborId));
	}

	@Transactional
	public void cancelOrRemove(UserPrincipal principal, Long neighborId) {
		Neighbor neighbor = requireNeighbor(neighborId);
		Long me = principal.getUserNo();
		boolean party = me.equals(neighbor.getRequesterUserNo()) || me.equals(neighbor.getReceiverUserNo());
		if (!party) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if ("PENDING".equals(neighbor.getStatus()) && !me.equals(neighbor.getRequesterUserNo())) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "대기 요청 취소는 요청자만 가능합니다. 거절은 reject를 사용하세요.");
		}
		if (!"PENDING".equals(neighbor.getStatus()) && !"ACCEPTED".equals(neighbor.getStatus())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "취소·해제할 수 있는 상태가 아닙니다.");
		}
		neighborMapper.deleteByNeighborId(neighborId);
		log.info("[cancelOrRemove] neighborId={} by={}", neighborId, me);
	}

	@Transactional
	public void block(UserPrincipal principal, Long targetUserNo) {
		Long me = principal.getUserNo();
		if (me.equals(targetUserNo)) {
			throw new BusinessException(ErrorCode.NEIGHBOR_SELF);
		}
		requireActiveUser(targetUserNo);
		if (userBlockMapper.findActiveByPair(me, targetUserNo) != null) {
			throw new BusinessException(ErrorCode.USER_ALREADY_BLOCKED);
		}

		UserBlock existing = userBlockMapper.findAnyByPair(me, targetUserNo);
		if (existing != null) {
			userBlockMapper.restore(existing.getUserBlockId());
		} else {
			UserBlock block = new UserBlock();
			block.setBlockerUserNo(me);
			block.setBlockedUserNo(targetUserNo);
			userBlockMapper.insert(block);
		}

		Neighbor neighbor = neighborMapper.findByPairKey(pairKey(me, targetUserNo));
		if (neighbor != null) {
			neighborMapper.deleteByNeighborId(neighbor.getNeighborId());
		}
		log.info("[block] me={} target={}", me, targetUserNo);
	}

	@Transactional
	public void unblock(UserPrincipal principal, Long targetUserNo) {
		Long me = principal.getUserNo();
		if (me.equals(targetUserNo)) {
			throw new BusinessException(ErrorCode.NEIGHBOR_SELF);
		}
		int updated = userBlockMapper.softDelete(me, targetUserNo);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.USER_NOT_BLOCKED);
		}
		log.info("[unblock] me={} target={}", me, targetUserNo);
	}

	/** 피드 NEIGHBORS 공개 범위 검사용 */
	@Transactional(readOnly = true)
	public boolean isAcceptedPair(Long userNoA, Long userNoB) {
		if (userNoA == null || userNoB == null || userNoA.equals(userNoB)) {
			return true;
		}
		return neighborMapper.countAcceptedPair(userNoA, userNoB) > 0;
	}

	public static String pairKey(Long userNoA, Long userNoB) {
		long a = userNoA;
		long b = userNoB;
		return a < b ? a + ":" + b : b + ":" + a;
	}

	private String resolveRelationStatus(Long me, Neighbor neighbor, boolean blockedByMe) {
		if (blockedByMe) {
			return "BLOCKED";
		}
		if (neighbor == null) {
			return "NONE";
		}
		if ("PENDING".equals(neighbor.getStatus())) {
			return me.equals(neighbor.getRequesterUserNo()) ? "PENDING_SENT" : "PENDING_RECEIVED";
		}
		if ("ACCEPTED".equals(neighbor.getStatus())) {
			return "ACCEPTED";
		}
		if ("REJECTED".equals(neighbor.getStatus())) {
			return "REJECTED";
		}
		if ("BLOCKED".equals(neighbor.getStatus())) {
			return "BLOCKED";
		}
		return "NONE";
	}

	private void assertNotBlockedEitherWay(Long userNoA, Long userNoB) {
		if (userBlockMapper.findActiveEitherWay(userNoA, userNoB) != null) {
			throw new BusinessException(ErrorCode.NEIGHBOR_BLOCKED);
		}
	}

	private Neighbor requireNeighbor(Long neighborId) {
		Neighbor neighbor = neighborMapper.findByNeighborId(neighborId);
		if (neighbor == null) {
			throw new BusinessException(ErrorCode.NEIGHBOR_NOT_FOUND);
		}
		return neighbor;
	}

	private User requireActiveUser(Long userNo) {
		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		return user;
	}

	private String normalizeListType(String type) {
		if (!StringUtils.hasText(type)) {
			return "RESIDENTS";
		}
		String value = type.trim().toUpperCase(Locale.ROOT);
		if ("RESIDENT".equals(value) || "FOLLOWING".equals(value)) {
			return "RESIDENTS";
		}
		if ("NEIGHBOR".equals(value) || "FOLLOWER".equals(value) || "FOLLOWERS".equals(value)) {
			return "NEIGHBORS";
		}
		if (!"RESIDENTS".equals(value) && !"NEIGHBORS".equals(value)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 RESIDENTS 또는 NEIGHBORS 입니다.");
		}
		return value;
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private NeighborRelationResponse toRelation(Neighbor neighbor) {
		return NeighborRelationResponse.builder()
				.neighborId(neighbor.getNeighborId())
				.requesterUserNo(neighbor.getRequesterUserNo())
				.receiverUserNo(neighbor.getReceiverUserNo())
				.status(neighbor.getStatus())
				.build();
	}
}
