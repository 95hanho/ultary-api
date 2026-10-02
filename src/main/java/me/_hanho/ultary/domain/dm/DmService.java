package me._hanho.ultary.domain.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.dm.dto.request.CreateDmRoomRequest;
import me._hanho.ultary.domain.dm.dto.request.SendDmMessageRequest;
import me._hanho.ultary.domain.dm.dto.response.DmMessageListResponse;
import me._hanho.ultary.domain.dm.dto.response.DmMessageResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomItemResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomListResponse;
import me._hanho.ultary.domain.dm.dto.response.DmShareResponse;
import me._hanho.ultary.domain.dm.model.DmMessage;
import me._hanho.ultary.domain.dm.model.DmMessageRow;
import me._hanho.ultary.domain.dm.model.DmRoom;
import me._hanho.ultary.domain.dm.model.DmRoomRow;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.user.UserBlockMapper;
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

	@Transactional(readOnly = true)
	public DmRoomListResponse listRooms(UserPrincipal principal, Integer limit) {
		List<DmRoomRow> rows = dmMapper.findRooms(principal.getUserNo(), resolveLimit(limit));
		return DmRoomListResponse.builder()
				.items(toRoomItems(rows))
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
		log.info("[leave] userNo={} roomId={}", principal.getUserNo(), roomId);
	}

	@Transactional
	public void read(UserPrincipal principal, Long roomId) {
		Long me = principal.getUserNo();
		requireOpenRoom(me, roomId);
		markReadLatest(me, roomId);
	}

	@Transactional
	public DmMessageListResponse messages(
			UserPrincipal principal, Long roomId, Long beforeMessageId, Integer limit) {
		Long me = principal.getUserNo();
		requireOpenRoom(me, roomId);
		int resolved = resolveLimit(limit);
		List<DmMessageRow> rows = dmMapper.findMessages(roomId, beforeMessageId, resolved + 1);
		boolean hasMore = rows.size() > resolved;
		if (hasMore) {
			rows = new ArrayList<>(rows.subList(0, resolved));
		}
		Collections.reverse(rows);
		Long nextCursor = hasMore ? rows.get(0).getDmMessageId() : null;
		markReadLatest(me, roomId);
		return DmMessageListResponse.builder()
				.items(toMessages(rows, me))
				.nextCursorMessageId(nextCursor)
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
		markReadLatest(me, roomId);
		log.info("[send] userNo={} roomId={} messageId={} share={}", me, roomId, message.getDmMessageId(), shareType);
		DmMessageRow row = dmMapper.findMessage(message.getDmMessageId());
		return toMessage(row, me);
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

	private void markReadLatest(Long userNo, Long roomId) {
		Long latest = dmMapper.findLatestMessageId(roomId);
		if (latest != null) {
			dmMapper.markRead(roomId, userNo, latest);
		}
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
		return toRoomItems(List.of(row)).get(0);
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

	private List<DmRoomItemResponse> toRoomItems(List<DmRoomRow> rows) {
		Set<Long> fileIds = new HashSet<>();
		for (DmRoomRow row : rows) {
			if (row.getProfileFileId() != null) {
				fileIds.add(row.getProfileFileId());
			}
		}
		var files = fileService.findSummaries(fileIds);
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
		List<DmMessageResponse> items = new ArrayList<>();
		for (DmMessageRow row : rows) {
			items.add(toMessage(row, me, files));
		}
		return items;
	}

	private DmMessageResponse toMessage(DmMessageRow row, Long me) {
		Set<Long> fileIds = new HashSet<>();
		collectFileIds(row, fileIds);
		return toMessage(row, me, fileService.findSummaries(fileIds));
	}

	private DmMessageResponse toMessage(
			DmMessageRow row, Long me, java.util.Map<Long, FileSummaryResponse> files) {
		return DmMessageResponse.builder()
				.dmMessageId(row.getDmMessageId())
				.senderUserNo(row.getSenderUserNo())
				.fromMe(me.equals(row.getSenderUserNo()))
				.body(StringUtils.hasText(row.getBody()) ? row.getBody().trim() : null)
				.createdAt(row.getCreatedAt())
				.share(toShare(row, files))
				.build();
	}

	private DmShareResponse toShare(DmMessageRow row, java.util.Map<Long, FileSummaryResponse> files) {
		if ("FEED".equals(row.getShareType())) {
			boolean available = row.getFeedDeleted() != null
					&& row.getFeedDeleted() == 0
					&& displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId()) != null;
			Long fileId = available
					? displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId())
					: null;
			return DmShareResponse.builder()
					.type("FEED")
					.available(available)
					.feedId(row.getFeedId())
					.feedMediaId(row.getFeedMediaId())
					.mediaIndex(available ? row.getMediaIndex() : null)
					.authorUserNo(available ? row.getAuthorUserNo() : null)
					.authorNickname(available ? row.getAuthorNickname() : null)
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
			return DmShareResponse.builder()
					.type("STORY")
					.available(available)
					.storyId(row.getStoryId())
					.file(fileId == null ? null : files.get(fileId))
					.build();
		}
		return null;
	}

	private void collectFileIds(DmMessageRow row, Set<Long> fileIds) {
		if ("FEED".equals(row.getShareType())) {
			add(fileIds, row.getAuthorProfileFileId());
			add(fileIds, displayFileId(row.getMediaType(), row.getMediaFileId(), row.getMediaThumbnailFileId()));
		} else if ("STORY".equals(row.getShareType())) {
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

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
