package me._hanho.ultary.domain.neighbor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.domain.neighbor.dto.response.BlockedUserItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborListItemResponse;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborRelationResponse;
import me._hanho.ultary.domain.neighbor.dto.response.UserUltaryResponse;
import me._hanho.ultary.domain.neighbor.model.BlockedUserRow;
import me._hanho.ultary.domain.neighbor.model.Neighbor;
import me._hanho.ultary.domain.neighbor.model.NeighborListRow;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.model.PetMention;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.report.ReportService;
import me._hanho.ultary.domain.report.dto.response.MyReportResponse;
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
	private final PrivacyService privacyService;
	private final UserMapper userMapper;
	private final PetMapper petMapper;
	private final PetService petService;
	private final FeedMapper feedMapper;
	private final StoryService storyService;
	private final FileService fileService;
	private final NotificationService notificationService;
	private final ReportService reportService;

	/** 울타리 조회와 같다. 없는 유저 404, 상대가 나를 차단하면 403 */
	@Transactional(readOnly = true)
	public void assertCanViewUltary(UserPrincipal principal, Long targetUserNo) {
		requireActiveUser(targetUserNo);
		if (principal.getUserNo().equals(targetUserNo)) {
			return;
		}
		if (userBlockMapper.findActiveByPair(targetUserNo, principal.getUserNo()) != null) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}
	}

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

		Integer profileFileId = petService.representativeProfileFileId(target.getUserNo());
		boolean hasStory = storyService.hasVisibleStory(principal.getUserNo(), targetUserNo);
		return UserUltaryResponse.builder()
				.userNo(target.getUserNo())
				.nickname(target.getNickname())
				.defaultNickname(Boolean.TRUE.equals(target.getIsDefaultNickname()))
				.profileFileId(profileFileId)
				.profileFile(fileService.findSummary(profileFileId))
				.bio(target.getBio())
				.regionSido(target.getRegionSido())
				.regionSigungu(target.getRegionSigungu())
				.hasStory(hasStory)
				.hasUnviewed(hasStory && storyService.hasUnviewedStory(targetUserNo, me))
				.followCount(neighborMapper.countAcceptedAsRequester(targetUserNo))
				.followerCount(neighborMapper.countAcceptedAsReceiver(targetUserNo))
				.petCount(petMapper.countActiveByUserNo(targetUserNo))
				.feedCount(feedMapper.countActiveByUserNo(targetUserNo))
				.relationStatus(relationStatus)
				.privateAccount(privacyService.isPrivateAccount(targetUserNo))
				.following(follows(me, neighbor))
				.followedBy(followedBy(me, neighbor))
				.neighborId(neighborId)
				.blockedByMe(blockedByMe)
				.blockedMe(false)
				.myReport(me.equals(targetUserNo) ? null : reportService.mineUser(me, targetUserNo))
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
		List<NeighborListRow> rows = "FOLLOWER".equals(listType)
				? neighborMapper.findAcceptedNeighbors(targetUserNo, resolvedLimit)
				: neighborMapper.findAcceptedResidents(targetUserNo, resolvedLimit);

		List<NeighborListItemResponse> result = new ArrayList<>();
		Set<Long> profileIds = new HashSet<>();
		for (NeighborListRow row : rows) {
			if (row.getProfileFileId() != null) {
				profileIds.add(row.getProfileFileId().longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		List<Long> userNos = new ArrayList<>();
		for (NeighborListRow row : rows) {
			userNos.add(row.getUserNo());
		}
		Map<Long, List<String>> petTags = petTagsByUser(userNos);
		Set<Long> followed = userNos.isEmpty()
				? Set.of()
				: new HashSet<>(neighborMapper.findFollowedUserNos(me, userNos));
		for (NeighborListRow row : rows) {
			Long profileId = row.getProfileFileId() == null ? null : row.getProfileFileId().longValue();
			boolean hasStory = storyService.hasVisibleStory(me, row.getUserNo());
			result.add(NeighborListItemResponse.builder()
					.neighborId(row.getNeighborId())
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.profileFileId(row.getProfileFileId())
					.profileFile(profileId == null ? null : files.get(profileId))
					.status(row.getStatus())
					.listType(row.getListType())
					.hasStory(hasStory)
					.hasUnviewed(hasStory && storyService.hasUnviewedStory(row.getUserNo(), me))
					.petTags(petTags.getOrDefault(row.getUserNo(), List.of()))
					.following(followed.contains(row.getUserNo()))
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
		privacyService.assertNeighborRequestOpen(targetUserNo);
		assertNotBlockedEitherWay(me, targetUserNo);

		String key = pairKey(me, targetUserNo);
		Neighbor existing = neighborMapper.findByPairKey(key);
		boolean instant = !privacyService.isPrivateAccount(targetUserNo);
		if (existing == null) {
			Neighbor created = new Neighbor();
			created.setRequesterUserNo(me);
			created.setReceiverUserNo(targetUserNo);
			created.setPairKey(key);
			created.setStatus(instant ? "ACCEPTED" : "PENDING");
			neighborMapper.insert(created);
			notificationService.syncNeighbor(created.getNeighborId(), true);
			log.info("[request] me={} target={} neighborId={} status={}",
					me, targetUserNo, created.getNeighborId(), created.getStatus());
			return toRelation(neighborMapper.findByNeighborId(created.getNeighborId()));
		}

		if ("ACCEPTED".equals(existing.getStatus())) {
			if (me.equals(existing.getRequesterUserNo()) || Boolean.TRUE.equals(existing.getFollowedBack())) {
				throw new BusinessException(ErrorCode.NEIGHBOR_ALREADY_EXISTS);
			}
			int updated = neighborMapper.updateFollowBack(existing.getNeighborId(), me);
			if (updated == 0) {
				throw new BusinessException(ErrorCode.NEIGHBOR_ALREADY_EXISTS);
			}
			notificationService.syncNeighbor(existing.getNeighborId(), false);
			log.info("[request:followBack] me={} target={} neighborId={}", me, targetUserNo, existing.getNeighborId());
			return toRelation(neighborMapper.findByNeighborId(existing.getNeighborId()));
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
		// REJECTED → 재요청. 공개 계정은 바로 ACCEPTED, 비공개 계정은 PENDING
		existing.setRequesterUserNo(me);
		existing.setReceiverUserNo(targetUserNo);
		existing.setStatus(instant ? "ACCEPTED" : "PENDING");
		neighborMapper.updateReRequest(existing);
		notificationService.syncNeighbor(existing.getNeighborId(), true);
		log.info("[request:re] me={} target={} neighborId={} status={}",
				me, targetUserNo, existing.getNeighborId(), existing.getStatus());
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
		notificationService.syncNeighbor(neighborId, false);
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
		if ("ACCEPTED".equals(neighbor.getStatus()) && Boolean.TRUE.equals(neighbor.getFollowedBack())) {
			if (me.equals(neighbor.getRequesterUserNo())) {
				int updated = neighborMapper.keepIncomingFollow(
						neighborId, neighbor.getReceiverUserNo(), neighbor.getRequesterUserNo());
				if (updated == 0) {
					throw new BusinessException(ErrorCode.NEIGHBOR_NOT_FOUND);
				}
				notificationService.syncNeighbor(neighborId, false);
				log.info("[unfollow:keepIncoming] neighborId={} by={}", neighborId, me);
				return;
			}
			int updated = neighborMapper.clearFollowBack(neighborId);
			if (updated == 0) {
				throw new BusinessException(ErrorCode.NEIGHBOR_NOT_FOUND);
			}
			notificationService.syncNeighbor(neighborId, false);
			log.info("[unfollow:clearBack] neighborId={} by={}", neighborId, me);
			return;
		}
		notificationService.removeByNeighbor(neighborId);
		neighborMapper.deleteByNeighborId(neighborId);
		log.info("[cancelOrRemove] neighborId={} by={}", neighborId, me);
	}

	@Transactional(readOnly = true)
	public List<BlockedUserItemResponse> listBlocked(UserPrincipal principal, Integer limit) {
		List<BlockedUserRow> rows = userBlockMapper.findBlockedUsers(principal.getUserNo(), resolveLimit(limit));
		Set<Long> profileIds = new HashSet<>();
		for (BlockedUserRow row : rows) {
			if (row.getProfileFileId() != null) {
				profileIds.add(row.getProfileFileId().longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		List<BlockedUserItemResponse> result = new ArrayList<>();
		for (BlockedUserRow row : rows) {
			Long profileId = row.getProfileFileId() == null ? null : row.getProfileFileId().longValue();
			result.add(BlockedUserItemResponse.builder()
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.profileFileId(row.getProfileFileId())
					.profileFile(profileId == null ? null : files.get(profileId))
					.blockedAt(row.getBlockedAt())
					.build());
		}
		return result;
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
			notificationService.removeByNeighbor(neighbor.getNeighborId());
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

	/** 조회자가 그 계정을 팔로우 중. 맞팔 포함. 상대만 팔로우한 ACCEPTED는 false */
	@Transactional(readOnly = true)
	public boolean viewerFollows(Long viewerUserNo, Long ownerUserNo) {
		if (viewerUserNo == null || ownerUserNo == null || viewerUserNo.equals(ownerUserNo)) {
			return false;
		}
		return neighborMapper.countViewerFollows(viewerUserNo, ownerUserNo) > 0;
	}

	/** userNos 중 조회자가 팔로우 중인 사람. ACCEPTED. 맞팔 포함 */
	@Transactional(readOnly = true)
	public Set<Long> followedUserNos(Long viewerUserNo, Collection<Long> userNos) {
		if (viewerUserNo == null || userNos == null || userNos.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(neighborMapper.findFollowedUserNos(viewerUserNo, userNos));
	}

	/** userNos 중 조회자를 팔로우 중인 사람. ACCEPTED. 맞팔 포함 */
	@Transactional(readOnly = true)
	public Set<Long> followerUserNos(Long viewerUserNo, Collection<Long> userNos) {
		if (viewerUserNo == null || userNos == null || userNos.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(neighborMapper.findFollowerUserNos(viewerUserNo, userNos));
	}

	/** 피드 NEIGHBORS 공개 범위 검사용. 어느 방향이든 ACCEPTED */
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
			return "FOLLOW";
		}
		String value = type.trim().toUpperCase(Locale.ROOT);
		if ("FOLLOW".equals(value) || "FOLLOWING".equals(value)
				|| "RESIDENTS".equals(value) || "RESIDENT".equals(value)) {
			return "FOLLOW";
		}
		if ("FOLLOWER".equals(value) || "FOLLOWERS".equals(value)
				|| "NEIGHBORS".equals(value) || "NEIGHBOR".equals(value)) {
			return "FOLLOWER";
		}
		throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 FOLLOW 또는 FOLLOWER 입니다.");
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
				.followedBack(Boolean.TRUE.equals(neighbor.getFollowedBack()))
				.build();
	}

	private boolean follows(Long me, Neighbor neighbor) {
		if (neighbor == null || !"ACCEPTED".equals(neighbor.getStatus())) {
			return false;
		}
		if (me.equals(neighbor.getRequesterUserNo())) {
			return true;
		}
		return me.equals(neighbor.getReceiverUserNo()) && Boolean.TRUE.equals(neighbor.getFollowedBack());
	}

	private boolean followedBy(Long me, Neighbor neighbor) {
		if (neighbor == null || !"ACCEPTED".equals(neighbor.getStatus())) {
			return false;
		}
		if (me.equals(neighbor.getReceiverUserNo())) {
			return true;
		}
		return me.equals(neighbor.getRequesterUserNo()) && Boolean.TRUE.equals(neighbor.getFollowedBack());
	}

	private Map<Long, List<String>> petTagsByUser(List<Long> userNos) {
		if (userNos.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<String>> tags = new HashMap<>();
		for (PetMention mention : petMapper.findActiveMentions(userNos)) {
			tags.computeIfAbsent(mention.getUserNo(), key -> new ArrayList<>()).add(mention.getMentionId());
		}
		return tags;
	}
}
