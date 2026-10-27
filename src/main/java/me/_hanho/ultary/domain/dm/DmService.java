package me._hanho.ultary.domain.dm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.dm.dto.request.CreateDmRoomRequest;
import me._hanho.ultary.domain.dm.dto.request.DmTypingRequest;
import me._hanho.ultary.domain.dm.dto.request.DmViewingRequest;
import me._hanho.ultary.domain.dm.dto.request.SendDmMessageRequest;
import me._hanho.ultary.domain.dm.dto.request.StartDmRequest;
import me._hanho.ultary.domain.dm.dto.response.StartDmResponse;
import me._hanho.ultary.domain.dm.dto.response.DmMessageListResponse;
import me._hanho.ultary.domain.dm.dto.response.DmMessageResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomItemResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomListResponse;
import me._hanho.ultary.domain.dm.dto.response.DmShareResponse;
import me._hanho.ultary.domain.dm.dto.response.DmShareTargetItemResponse;
import me._hanho.ultary.domain.dm.dto.response.DmShareTargetListResponse;
import me._hanho.ultary.domain.dm.model.DmMessage;
import me._hanho.ultary.domain.dm.model.DmMessageRow;
import me._hanho.ultary.domain.dm.model.DmRoom;
import me._hanho.ultary.domain.dm.model.DmRoomRow;
import me._hanho.ultary.domain.dm.model.ShareTargetRow;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.domain.ws.RealtimePush;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DmService {

	private static final int DEFAULT_LIMIT = 30;
	private static final int MAX_LIMIT = 50;
	private static final int BODY_MAX = 1000;

	private final DmMapper dmMapper;
	private final UserMapper userMapper;
	private final UserBlockMapper userBlockMapper;
	private final NeighborService neighborService;
	private final FeedService feedService;
	private final FeedMapper feedMapper;
	private final StoryService storyService;
	private final FileService fileService;
	private final RealtimePush realtimePush;
	private final DmPresence presence;
	private final PetMapper petMapper;

	@Transactional(readOnly = true)
	public DmRoomListResponse listRooms(UserPrincipal principal, Integer limit) {
		List<DmRoomRow> rows = dmMapper.findRooms(principal.getUserNo(), resolveLimit(limit));
		return DmRoomListResponse.builder()
				.items(toRoomItems(principal.getUserNo(), rows))
				.build();
	}

	@Transactional
	public DmRoomItemResponse createRoom(UserPrincipal principal, CreateDmRoomRequest request) {
		Long me = principal.getUserNo();
		Long target = request == null ? null : request.getTargetUserNo();
		if (target == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "대화 상대를 선택해 주세요.");
		}
		if (me.equals(target)) {
			throw new BusinessException(ErrorCode.DM_SELF);
		}
		User peer = userMapper.findActiveByUserNo(target);
		if (peer == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		assertNotBlocked(me, target);
		assertNeighbor(me, target);

		String key = NeighborService.pairKey(me, target);
		DmRoom room = dmMapper.findByPairKey(key);
		if (room == null) {
			room = new DmRoom();
			room.setPairKey(key);
			room.setUserLow(Math.min(me, target));
			room.setUserHigh(Math.max(me, target));
			try {
				dmMapper.insertRoom(room);
			} catch (DuplicateKeyException ex) {
				room = dmMapper.findByPairKey(key);
			}
		}
		if (room == null) {
			throw new BusinessException(ErrorCode.DM_ROOM_NOT_FOUND);
		}
		dmMapper.reopen(room.getDmRoomId(), me);
		log.info("[createRoom] me={} target={} roomId={}", me, target, room.getDmRoomId());
		return requireRoomItem(me, room.getDmRoomId());
	}

	@Transactional
	public void leave(UserPrincipal principal, Long roomId) {
		DmRoom room = requireMember(principal.getUserNo(), roomId);
		dmMapper.leave(room.getDmRoomId(), principal.getUserNo());
		presence.leave(principal.getUserNo(), room.getDmRoomId());
		log.info("[leave] userNo={} roomId={}", principal.getUserNo(), roomId);
	}

	/** 첫 전송이 실패하면 새 방 생성과 기존 방 재입장도 함께 롤백한다. */
	@Transactional
	public StartDmResponse start(UserPrincipal principal, StartDmRequest request) {
		CreateDmRoomRequest roomRequest = new CreateDmRoomRequest();
		roomRequest.setTargetUserNo(request == null ? null : request.getTargetUserNo());
		DmRoomItemResponse room = createRoom(principal, roomRequest);
		DmMessageResponse message = send(principal, room.getDmRoomId(), request);
		return new StartDmResponse(requireRoomItem(principal.getUserNo(), room.getDmRoomId()), message);
	}

	@Transactional
	public void read(UserPrincipal principal, Long roomId) {
		Long me = principal.getUserNo();
		DmRoom room = requireOpenRoom(me, roomId);
		markReadLatest(room, me);
	}

	/** 이 방 화면을 보고 있는 동안 true를 약 10초마다 다시 보낸다. 20초가 지나면 나간 것으로 본다 */
	@Transactional
	public void viewing(UserPrincipal principal, Long roomId, DmViewingRequest request) {
		if (request == null || request.getViewing() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "보고 있는지 알려 주세요.");
		}
		Long me = principal.getUserNo();
		DmRoom room = requireOpenRoom(me, roomId);
		if (Boolean.TRUE.equals(request.getViewing())) {
			presence.view(me, roomId);
			markReadLatest(room, me);
		} else {
			presence.leave(me, roomId);
		}
		log.info("[viewing] userNo={} roomId={} viewing={}", me, roomId, request.getViewing());
	}

	@Transactional
	public void typing(UserPrincipal principal, Long roomId, DmTypingRequest request) {
		if (request == null || request.getTyping() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "입력 중인지 알려 주세요.");
		}
		Long me = principal.getUserNo();
		DmRoom room = requireOpenRoom(me, roomId);
		presence.typing(me, peerOf(room, me), roomId, Boolean.TRUE.equals(request.getTyping()));
		log.info("[typing] userNo={} roomId={} typing={}", me, roomId, request.getTyping());
	}

	@Transactional
	public DmMessageListResponse messages(
			UserPrincipal principal, Long roomId, Long beforeMessageId, Integer limit) {
		Long me = principal.getUserNo();
		DmRoom room = requireOpenRoom(me, roomId);
		int resolved = resolveLimit(limit);
		Long firstUnread = beforeMessageId == null
				? dmMapper.findFirstUnreadMessageId(roomId, me, readCursor(room, me)) : null;
		List<DmMessageRow> rows = dmMapper.findMessages(roomId, beforeMessageId, resolved + 1);
		boolean hasMore = rows.size() > resolved;
		if (hasMore) {
			rows = new ArrayList<>(rows.subList(0, resolved));
		}
		Collections.reverse(rows);
		Long nextCursor = hasMore ? rows.get(0).getDmMessageId() : null;
		Long peer = peerOf(room, me);
		Long peerLastRead = readCursor(room, peer);
		markReadLatest(room, me);
		return DmMessageListResponse.builder()
				.items(toMessages(rows, me))
				.nextCursorMessageId(nextCursor)
				.peerLastReadMessageId(peerLastRead)
				.firstUnreadMessageId(firstUnread)
				.following(neighborService.viewerFollows(me, peer))
				.followedBy(neighborService.viewerFollows(peer, me))
				.build();
	}

	@Transactional
	public DmMessageResponse send(UserPrincipal principal, Long roomId, SendDmMessageRequest request) {
		Long me = principal.getUserNo();
		DmRoom room = requireOpenRoom(me, roomId);
		Long peer = peerOf(room, me);
		assertNotBlocked(me, peer);
		assertNeighbor(me, peer);

		String body = normalizeBody(request == null ? null : request.getBody());
		Long feedId = request == null ? null : request.getFeedId();
		Long feedMediaId = request == null ? null : request.getFeedMediaId();
		Long storyId = request == null ? null : request.getStoryId();
		if (feedId != null && storyId != null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "게시글과 스토리는 함께 보낼 수 없습니다.");
		}
		if (feedMediaId != null && feedId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "사진을 지정하려면 게시글이 필요합니다.");
		}
		String shareType = "NONE";
		if (feedId != null) {
			feedMediaId = resolveFeedMedia(feedId, feedMediaId, me);
			shareType = "FEED";
		} else if (storyId != null) {
			storyService.assertViewable(storyId, me);
			shareType = "STORY";
		} else if (body == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "메시지를 입력해 주세요.");
		}

		DmMessage message = new DmMessage();
		message.setDmRoomId(roomId);
		message.setSenderUserNo(me);
		message.setBody(body);
		message.setShareType(shareType);
		message.setFeedId(feedId);
		message.setFeedMediaId(feedMediaId);
		message.setStoryId(storyId);
		dmMapper.insertMessage(message);
		dmMapper.clearPeerLeft(roomId, me);
		markReadLatest(room, me);
		if (presence.isViewing(peer, roomId)) {
			markReadUpTo(room, peer, message.getDmMessageId());
		}
		presence.typing(me, peer, roomId, false);
		log.info("[send] userNo={} roomId={} messageId={} share={}", me, roomId, message.getDmMessageId(), shareType);
		DmMessageRow row = dmMapper.findMessage(message.getDmMessageId());
		realtimePush.pushDm(me, roomItemOrNull(me, roomId), toMessage(row, me));
		realtimePush.pushDm(peer, roomItemOrNull(peer, roomId), toMessage(row, peer));
		return toMessage(row, me);
	}

	private DmRoomItemResponse roomItemOrNull(Long userNo, Long roomId) {
		DmRoomRow row = dmMapper.findRoomRow(userNo, roomId);
		if (row == null) {
			return null;
		}
		return toRoomItems(userNo, List.of(row)).get(0);
	}

	private Long resolveFeedMedia(Long feedId, Long feedMediaId, Long viewerUserNo) {
		feedService.assertVisible(feedId, viewerUserNo);
		List<FeedMedia> media = feedMapper.findMediaByFeedId(feedId);
		if (media.isEmpty()) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		if (feedMediaId == null) {
			return media.get(0).getFeedMediaId();
		}
		for (FeedMedia item : media) {
			if (feedMediaId.equals(item.getFeedMediaId())) {
				return item.getFeedMediaId();
			}
		}
		throw new BusinessException(ErrorCode.INVALID_INPUT, "공유할 게시글 사진을 찾을 수 없습니다.");
	}

	private void markReadLatest(DmRoom room, Long userNo) {
		markReadUpTo(room, userNo, dmMapper.findLatestMessageId(room.getDmRoomId()));
	}

	/** 커서가 실제로 올라갈 때만 상대에게 DM_READ를 보낸다 */
	private void markReadUpTo(DmRoom room, Long userNo, Long messageId) {
		if (messageId == null) {
			return;
		}
		Long current = readCursor(room, userNo);
		if (current != null && current >= messageId) {
			return;
		}
		dmMapper.markRead(room.getDmRoomId(), userNo, messageId);
		if (userNo.equals(room.getUserLow())) {
			room.setLowLastReadMessageId(messageId);
		} else {
			room.setHighLastReadMessageId(messageId);
		}
		realtimePush.pushDmRead(peerOf(room, userNo), room.getDmRoomId(), messageId);
	}

	private Long readCursor(DmRoom room, Long userNo) {
		Long cursor = userNo.equals(room.getUserLow())
				? room.getLowLastReadMessageId()
				: room.getHighLastReadMessageId();
		return cursor == null || cursor <= 0 ? null : cursor;
	}

	private DmRoom requireOpenRoom(Long userNo, Long roomId) {
		DmRoom room = requireMember(userNo, roomId);
		if (leftAt(room, userNo) != null) {
			throw new BusinessException(ErrorCode.DM_ROOM_NOT_FOUND);
		}
		assertNotBlocked(userNo, peerOf(room, userNo));
		return room;
	}

	private DmRoom requireMember(Long userNo, Long roomId) {
		DmRoom room = roomId == null ? null : dmMapper.findById(roomId);
		if (room == null || (!userNo.equals(room.getUserLow()) && !userNo.equals(room.getUserHigh()))) {
			throw new BusinessException(ErrorCode.DM_ROOM_NOT_FOUND);
		}
		return room;
	}

	private DmRoomItemResponse requireRoomItem(Long userNo, Long roomId) {
		DmRoomRow row = dmMapper.findRoomRow(userNo, roomId);
		if (row == null) {
			throw new BusinessException(ErrorCode.DM_ROOM_NOT_FOUND);
		}
		return toRoomItems(userNo, List.of(row)).get(0);
	}

	private void assertNeighbor(Long me, Long target) {
		if (!neighborService.isAcceptedPair(me, target)) {
			throw new BusinessException(ErrorCode.DM_NOT_NEIGHBOR);
		}
	}

	private void assertNotBlocked(Long me, Long target) {
		if (userBlockMapper.findActiveByPair(me, target) != null
				|| userBlockMapper.findActiveByPair(target, me) != null) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}
	}

	private List<DmRoomItemResponse> toRoomItems(Long viewerUserNo, List<DmRoomRow> rows) {
		Set<Long> fileIds = new HashSet<>();
		List<Long> peerUserNos = new ArrayList<>();
		for (DmRoomRow row : rows) {
			if (row.getProfileFileId() != null) {
				fileIds.add(row.getProfileFileId());
			}
			if (row.getPeerUserNo() != null) {
				peerUserNos.add(row.getPeerUserNo());
			}
		}
		var files = fileService.findSummaries(fileIds);
		Set<Long> following = neighborService.followedUserNos(viewerUserNo, peerUserNos);
		Set<Long> followedBy = neighborService.followerUserNos(viewerUserNo, peerUserNos);
		List<DmRoomItemResponse> items = new ArrayList<>();
		for (DmRoomRow row : rows) {
			items.add(DmRoomItemResponse.builder()
					.dmRoomId(row.getDmRoomId())
					.peerUserNo(row.getPeerUserNo())
					.peerNickname(row.getPeerNickname())
					.profileFile(row.getProfileFileId() == null ? null : files.get(row.getProfileFileId()))
					.lastMessage(preview(row))
					.lastMessageAt(row.getLastMessageAt())
					.unreadCount(row.getUnreadCount() == null ? 0 : row.getUnreadCount().intValue())
					.following(following.contains(row.getPeerUserNo()))
					.followedBy(followedBy.contains(row.getPeerUserNo()))
					.build());
		}
		return items;
	}

	private String preview(DmRoomRow row) {
		if (row.getLastShareType() == null) {
			return null;
		}
		if (StringUtils.hasText(row.getLastBody())) {
			return row.getLastBody().trim();
		}
		if ("FEED".equals(row.getLastShareType())) {
			return "게시글을 공유했습니다";
		}
		if ("STORY".equals(row.getLastShareType())) {
			return "스토리를 공유했습니다";
		}
		return null;
	}

	private List<DmMessageResponse> toMessages(List<DmMessageRow> rows, Long me) {
		Set<Long> fileIds = new HashSet<>();
		for (DmMessageRow row : rows) {
			collectFileIds(row, fileIds);
		}
		var files = fileService.findSummaries(fileIds);
		var authorStories = new java.util.HashMap<Long, SharedAuthorStory>();
		List<DmMessageResponse> items = new ArrayList<>();
		for (DmMessageRow row : rows) {
			items.add(toMessage(row, me, files, authorStories));
		}
		return items;
	}

	private DmMessageResponse toMessage(DmMessageRow row, Long me) {
		Set<Long> fileIds = new HashSet<>();
		collectFileIds(row, fileIds);
		return toMessage(row, me, fileService.findSummaries(fileIds), new java.util.HashMap<>());
	}

	private DmMessageResponse toMessage(
			DmMessageRow row, Long me, java.util.Map<Long, FileSummaryResponse> files,
			java.util.Map<Long, SharedAuthorStory> authorStories) {
		return DmMessageResponse.builder()
				.dmMessageId(row.getDmMessageId())
				.senderUserNo(row.getSenderUserNo())
				.fromMe(me.equals(row.getSenderUserNo()))
				.body(StringUtils.hasText(row.getBody()) ? row.getBody().trim() : null)
				.createdAt(row.getCreatedAt())
				.share(toShare(row, me, files, authorStories))
				.build();
	}

	private DmShareResponse toShare(DmMessageRow row, Long me,
			java.util.Map<Long, FileSummaryResponse> files,
			java.util.Map<Long, SharedAuthorStory> authorStories) {
		if ("FEED".equals(row.getShareType())) {
			boolean available = row.getFeedDeleted() != null
					&& row.getFeedDeleted() == 0
					&& displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId()) != null;
			Long fileId = available
					? displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId())
					: null;
			SharedAuthorStory authorStory = sharedAuthorStory(available ? row.getAuthorUserNo() : null, me, authorStories);
			return DmShareResponse.builder()
					.type("FEED")
					.available(available)
					.feedId(row.getFeedId())
					.feedMediaId(row.getFeedMediaId())
					.mediaIndex(available ? row.getMediaIndex() : null)
					.authorUserNo(available ? row.getAuthorUserNo() : null)
					.authorNickname(available ? row.getAuthorNickname() : null)
					.authorHasStory(authorStory.hasStory())
					.authorHasUnviewed(authorStory.hasUnviewed())
					.authorProfileFile(available && row.getAuthorProfileFileId() != null
							? files.get(row.getAuthorProfileFileId())
							: null)
					.content(available && StringUtils.hasText(row.getFeedContent()) ? row.getFeedContent() : null)
					.file(fileId == null ? null : files.get(fileId))
					.build();
		}
		if ("STORY".equals(row.getShareType())) {
			boolean available = row.getStoryDeleted() != null
					&& row.getStoryDeleted() == 0
					&& displayFileId(row.getStoryMediaType(), row.getStoryFileId(), row.getStoryThumbnailFileId()) != null;
			Long fileId = available
					? displayFileId(row.getStoryMediaType(), row.getStoryFileId(), row.getStoryThumbnailFileId())
					: null;
			SharedAuthorStory authorStory = sharedAuthorStory(available ? row.getAuthorUserNo() : null, me, authorStories);
			return DmShareResponse.builder()
					.type("STORY")
					.available(available)
					.storyId(row.getStoryId())
					.authorUserNo(available ? row.getAuthorUserNo() : null)
					.authorNickname(available ? row.getAuthorNickname() : null)
					.authorProfileFile(available && row.getAuthorProfileFileId() != null
							? files.get(row.getAuthorProfileFileId()) : null)
					.authorHasStory(authorStory.hasStory())
					.authorHasUnviewed(authorStory.hasUnviewed())
					.file(fileId == null ? null : files.get(fileId))
					.build();
		}
		return null;
	}

	private record SharedAuthorStory(boolean hasStory, boolean hasUnviewed) {}

	private SharedAuthorStory sharedAuthorStory(Long owner, Long viewer,
			java.util.Map<Long, SharedAuthorStory> cache) {
		if (owner == null) return new SharedAuthorStory(false, false);
		return cache.computeIfAbsent(owner, userNo -> {
			boolean visible = storyService.hasVisibleStory(viewer, userNo);
			return new SharedAuthorStory(visible, visible && storyService.hasUnviewedStory(userNo, viewer));
		});
	}

	private void collectFileIds(DmMessageRow row, Set<Long> fileIds) {
		if ("FEED".equals(row.getShareType())) {
			add(fileIds, row.getAuthorProfileFileId());
			add(fileIds, displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId()));
		} else if ("STORY".equals(row.getShareType())) {
			add(fileIds, row.getAuthorProfileFileId());
			add(fileIds, displayFileId(row.getStoryMediaType(), row.getStoryFileId(), row.getStoryThumbnailFileId()));
		}
	}

	private void add(Set<Long> fileIds, Long fileId) {
		if (fileId != null) {
			fileIds.add(fileId);
		}
	}

	private Long displayFileId(String mediaType, Long fileId, Long thumbnailFileId) {
		if ("VIDEO".equals(mediaType) && thumbnailFileId != null) {
			return thumbnailFileId;
		}
		return fileId;
	}

	private String normalizeBody(String body) {
		if (!StringUtils.hasText(body)) {
			return null;
		}
		String value = body.trim();
		if (value.length() > BODY_MAX) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "메시지는 1000자까지 입력할 수 있습니다.");
		}
		return value;
	}

	private Long peerOf(DmRoom room, Long me) {
		return me.equals(room.getUserLow()) ? room.getUserHigh() : room.getUserLow();
	}

	private java.time.LocalDateTime leftAt(DmRoom room, Long me) {
		return me.equals(room.getUserLow()) ? room.getLowLeftAt() : room.getHighLeftAt();
	}

	@Transactional(readOnly = true)
	public DmShareTargetListResponse shareTargets(UserPrincipal principal, Integer limit, String query, String priorityOrder) {
		return shareTargets(principal, limit, query, priorityOrder, "SHARE");
	}

	@Transactional(readOnly = true)
	public DmShareTargetListResponse shareTargets(UserPrincipal principal, Integer limit, String query, String priorityOrder, String mode) {
        return shareTargets(principal,limit,query,priorityOrder,mode,null);
    }
    @Transactional(readOnly = true)
    public DmShareTargetListResponse shareTargets(UserPrincipal principal, Integer limit, String query, String priorityOrder, String mode, Long feedId) {
		if (!Set.of("SHARE", "DM", "STORY_TAG", "FEED_TAG", "COMMENT_MENTION").contains(mode)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 목록 모드입니다.");
		}
		boolean tagging = "STORY_TAG".equals(mode) || "FEED_TAG".equals(mode);
		String defaultOrder = switch (mode) {
			case "STORY_TAG" -> "OWN_STORY_TAG,DM,OWN_FEED_TAG";
			case "FEED_TAG" -> "OWN_FEED_TAG,DM,OWN_STORY_TAG";
			default -> null;
		};
		List<String> baseOrder = sharePriorityOrder(StringUtils.hasText(priorityOrder) ? priorityOrder : defaultOrder);
        List<String> order = new ArrayList<>();
        if ("COMMENT_MENTION".equals(mode)) order.add("MENTION");
        for (String priority : baseOrder) {
            order.add(tagging && "DM".equals(priority) ? "DM_OR_MENTION" : priority);
            if (!tagging && !"COMMENT_MENTION".equals(mode) && "DM".equals(priority)) order.add("MENTION");
        }
		String term = query == null ? "" : query.trim().replaceFirst("^@+", "").toLowerCase(Locale.ROOT);
		Long participantFeed = "COMMENT_MENTION".equals(mode) ? feedId : null;
        if (participantFeed != null) feedService.assertVisible(participantFeed, principal.getUserNo());
		LocalDateTime cutoff = LocalDateTime.now().minusMonths(1);
		List<ShareTargetRow> rows = dmMapper.findShareCandidates(principal.getUserNo(), cutoff, term, tagging, "COMMENT_MENTION".equals(mode), participantFeed);
		List<RankedTarget> ranked = new ArrayList<>();
		for (ShareTargetRow row : rows) {
			if (row.getUserNo() == null) {
				continue;
			}
			RankedTarget target = tagging && row.getUserNo().equals(principal.getUserNo())
					? new RankedTarget(row, 99, cutoff) : rank(row, cutoff, order, "COMMENT_MENTION".equals(mode));
			if (target != null) {
				ranked.add(target);
			}
		}
		ranked.sort(Comparator
				.comparing((RankedTarget target) -> target.group() == 99)
                .thenComparing(target -> target.row().getFollowedAt() == null)
                .thenComparingInt(RankedTarget::group)
                .thenComparingInt(RankedTarget::mentionOrder)
				.thenComparing(RankedTarget::activeAt, Comparator.reverseOrder())
				.thenComparing(target -> target.row().getUserNo(), Comparator.reverseOrder()));
		int size = Math.min(ranked.size(), resolveLimit(limit));
		Set<Long> fileIds = new HashSet<>();
		for (int i = 0; i < size; i++) {
			Long profileFileId = ranked.get(i).row().getProfileFileId();
			if (profileFileId != null) {
				fileIds.add(profileFileId);
			}
		}
		var files = fileService.findSummaries(fileIds);
		Map<Long, List<String>> petTags = new HashMap<>();
		Map<Long, List<DmShareTargetItemResponse.PetChoice>> petChoices = new HashMap<>();
		List<Long> userNos = ranked.subList(0, size).stream().map(t -> t.row().getUserNo()).toList();
		if (!userNos.isEmpty()) {
			for (var pet : petMapper.findActiveMentions(userNos)) {
				petTags.computeIfAbsent(pet.getUserNo(), key -> new ArrayList<>()).add(pet.getMentionId());
				petChoices.computeIfAbsent(pet.getUserNo(), key -> new ArrayList<>())
						.add(new DmShareTargetItemResponse.PetChoice(pet.getPetId(), pet.getMentionId()));
			}
		}
		List<DmShareTargetItemResponse> items = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			RankedTarget target = ranked.get(i);
			ShareTargetRow row = target.row();
			items.add(DmShareTargetItemResponse.builder()
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.petTags(petTags.getOrDefault(row.getUserNo(), List.of()))
					.pets(petChoices.getOrDefault(row.getUserNo(), List.of()))
					.profileFile(row.getProfileFileId() == null ? null : files.get(row.getProfileFileId()))
					.dmRoomId(row.getDmRoomId())
					.following(row.getFollowedAt() != null)
					.followedBy(row.getFollowerAt() != null)
					.group(target.group())
					.activeAt(target.activeAt())
					.build());
		}
		return DmShareTargetListResponse.builder().items(items).build();
	}

	private static final List<String> DEFAULT_SHARE_PRIORITY = List.of("DM", "OWN_STORY_TAG", "OWN_FEED_TAG");

	private List<String> sharePriorityOrder(String value) {
		if (!StringUtils.hasText(value)) return DEFAULT_SHARE_PRIORITY;
		List<String> order = Arrays.stream(value.split(",", -1)).map(String::trim).toList();
		if (order.size() != 3 || !new HashSet<>(order).equals(new HashSet<>(DEFAULT_SHARE_PRIORITY))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"priorityOrder는 DM,OWN_STORY_TAG,OWN_FEED_TAG를 중복 없이 지정해 주세요.");
		}
		return order;
	}

    private RankedTarget rank(ShareTargetRow row, LocalDateTime cutoff, List<String> order, boolean commentMode) {
        for (int i=0; i<order.size(); i++) {
            String priority = order.get(i);
            if ("MENTION".equals(priority)) {
                if (withinMonth(row.getOwnMentionAt(), cutoff)) return new RankedTarget(row,i+1,row.getOwnMentionAt(),0);
                if (withinMonth(row.getIncomingMentionAt(), cutoff)) return new RankedTarget(row,i+1,row.getIncomingMentionAt(),1);
                continue;
            }
            LocalDateTime at = switch(priority) {
                case "DM" -> row.getLastMessageAt();
                case "DM_OR_MENTION" -> newest(row.getLastMessageAt(),row.getOwnMentionAt(),row.getIncomingMentionAt());
                case "OWN_STORY_TAG" -> row.getOwnStoryTagAt();
                case "OWN_FEED_TAG" -> row.getOwnFeedTagAt();
                default -> null;
            };
            if (withinMonth(at,cutoff)) return new RankedTarget(row,i+1,at);
        }
        int next = order.size()+1;
        if (withinMonth(row.getTaggedInStoryAt(),cutoff)) return new RankedTarget(row,next,row.getTaggedInStoryAt());
        if (withinMonth(row.getTaggedInFeedAt(),cutoff)) return new RankedTarget(row,next+1,row.getTaggedInFeedAt());
        if (withinMonth(row.getFollowedAt(),cutoff)) return new RankedTarget(row,next+2,row.getFollowedAt());
        if (withinMonth(row.getFollowerAt(),cutoff)) return new RankedTarget(row,next+3,row.getFollowerAt());
        if (row.getFollowedAt()!=null) return new RankedTarget(row,next+4,row.getFollowedAt());
        if (commentMode && row.getParticipantAt()!=null) return new RankedTarget(row,next+5,row.getParticipantAt());
        if (row.getFollowerAt()!=null) return new RankedTarget(row,next+(commentMode?6:5),row.getFollowerAt());
        return null;
    }
    private static LocalDateTime newest(LocalDateTime... values) {
        return Arrays.stream(values).filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }

	private static boolean withinMonth(LocalDateTime at, LocalDateTime cutoff) {
		return at != null && !at.isBefore(cutoff);
	}

	private record RankedTarget(ShareTargetRow row, int group, LocalDateTime activeAt, int mentionOrder) {
        RankedTarget(ShareTargetRow row, int group, LocalDateTime activeAt) { this(row,group,activeAt,0); }
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
