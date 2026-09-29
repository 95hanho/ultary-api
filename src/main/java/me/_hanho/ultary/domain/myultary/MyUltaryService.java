package me._hanho.ultary.domain.myultary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.myultary.dto.request.UpdateMyBioRequest;
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.myultary.dto.response.MyUltaryProfileResponse;
import me._hanho.ultary.domain.neighbor.NeighborMapper;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyUltaryService {

	private static final int DEFAULT_LIMIT = 30;
	private static final int MAX_LIMIT = 50;

	private final UserMapper userMapper;
	private final NeighborMapper neighborMapper;
	private final PetMapper petMapper;
	private final PetService petService;
	private final FeedMapper feedMapper;
	private final FeedService feedService;
	private final FileService fileService;
	private final StoryService storyService;

	@Transactional(readOnly = true)
	public MyUltaryProfileResponse getProfile(UserPrincipal principal) {
		User user = requireUser(principal.getUserNo());
		Integer profileFileId = petService.representativeProfileFileId(user.getUserNo());
		return MyUltaryProfileResponse.builder()
				.userNo(user.getUserNo())
				.nickname(user.getNickname())
				.defaultNickname(Boolean.TRUE.equals(user.getIsDefaultNickname()))
				.profileFileId(profileFileId)
				.profileFile(fileService.findSummary(profileFileId))
				.bio(user.getBio())
				.regionSido(user.getRegionSido())
				.regionSigungu(user.getRegionSigungu())
				.hasStory(storyService.hasActiveStory(user.getUserNo()))
				.hasUnviewed(storyService.hasUnviewedStory(user.getUserNo(), principal.getUserNo()))
				.residentCount(neighborMapper.countAcceptedAsRequester(user.getUserNo()))
				.neighborCount(neighborMapper.countAcceptedAsReceiver(user.getUserNo()))
				.petCount(petMapper.countActiveByUserNo(user.getUserNo()))
				.feedCount(feedMapper.countActiveByUserNo(user.getUserNo()))
				.build();
	}

	@Transactional(readOnly = true)
	public List<FeedGridItemResponse> getFeeds(UserPrincipal principal, Integer limit) {
		return toGridItems(feedMapper.findActiveByUserNo(principal.getUserNo(), resolveLimit(limit)));
	}

	/** 그 유저의 게시글 그리드. 본인이면 내 그리드와 같다 */
	@Transactional(readOnly = true)
	public List<FeedGridItemResponse> getFeedsOf(UserPrincipal principal, Long ownerUserNo, Integer limit) {
		if (principal.getUserNo().equals(ownerUserNo)) {
			return getFeeds(principal, limit);
		}
		return toGridItems(feedMapper.findVisibleByOwner(
				ownerUserNo, principal.getUserNo(), resolveLimit(limit)));
	}

	@Transactional(readOnly = true)
	public FeedResponse getFeedDetail(UserPrincipal principal, Long feedId) {
		Feed feed = feedMapper.findActiveByFeedId(feedId);
		if (feed == null || !feed.getUserNo().equals(principal.getUserNo())) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		return feedService.getDetail(principal, feedId);
	}

	@Transactional(readOnly = true)
	public List<FeedGridItemResponse> getSavedFeeds(UserPrincipal principal, Integer limit) {
		return toGridItems(feedMapper.findSavedByUserNo(principal.getUserNo(), resolveLimit(limit)));
	}

	@Transactional(readOnly = true)
	public List<FeedGridItemResponse> getTaggedFeeds(UserPrincipal principal, Integer limit) {
		return toGridItems(feedMapper.findTaggedByUserNo(principal.getUserNo(), resolveLimit(limit)));
	}

	@Transactional
	public MyUltaryProfileResponse updateBio(UserPrincipal principal, UpdateMyBioRequest request) {
		boolean clear = Boolean.TRUE.equals(request.getClearBio());
		if (!clear && request.getBio() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "bio 또는 clearBio가 필요합니다.");
		}
		String bio = clear ? null : (StringUtils.hasText(request.getBio()) ? request.getBio().trim() : "");
		int updated = userMapper.updateBio(principal.getUserNo(), bio, clear);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		log.info("[updateBio] userNo={} clear={}", principal.getUserNo(), clear);
		return getProfile(principal);
	}

	@Transactional(readOnly = true)
	public List<StoryResponse> listMyStories(UserPrincipal principal) {
		return storyService.listMine(principal);
	}

	@Transactional
	public StoryResponse createStory(UserPrincipal principal, CreateStoryRequest request) {
		return storyService.create(principal, request);
	}

	@Transactional
	public void deleteStory(UserPrincipal principal, Long storyId) {
		storyService.delete(principal, storyId);
	}

	private List<FeedGridItemResponse> toGridItems(List<Feed> feeds) {
		if (feeds == null || feeds.isEmpty()) {
			return List.of();
		}
		Set<Long> fileIds = new HashSet<>();
		List<FeedMedia> covers = new ArrayList<>();
		List<Integer> mediaCounts = new ArrayList<>();
		for (Feed feed : feeds) {
			List<FeedMedia> mediaList = feedMapper.findMediaByFeedId(feed.getFeedId());
			FeedMedia cover = mediaList.isEmpty() ? null : mediaList.get(0);
			covers.add(cover);
			mediaCounts.add(mediaList.size());
			if (cover != null) {
				if (cover.getFileId() != null) {
					fileIds.add(cover.getFileId());
				}
				if (cover.getThumbnailFileId() != null) {
					fileIds.add(cover.getThumbnailFileId());
				}
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		List<FeedGridItemResponse> items = new ArrayList<>();
		for (int i = 0; i < feeds.size(); i++) {
			Feed feed = feeds.get(i);
			FeedMedia cover = covers.get(i);
			items.add(FeedGridItemResponse.builder()
					.feedId(feed.getFeedId())
					.coverFileId(cover != null ? cover.getFileId() : null)
					.coverFile(cover == null || cover.getFileId() == null
							? null
							: files.get(cover.getFileId()))
					.coverThumbnailFileId(cover != null ? cover.getThumbnailFileId() : null)
					.coverThumbnailFile(cover == null || cover.getThumbnailFileId() == null
							? null
							: files.get(cover.getThumbnailFileId()))
					.coverMediaType(cover != null ? cover.getMediaType() : null)
					.mediaCount(mediaCounts.get(i))
					.likeCount(feed.getLikeCount())
					.commentCount(feed.getCommentCount())
					.createdAt(feed.getCreatedAt())
					.build());
		}
		return items;
	}

	private User requireUser(Long userNo) {
		User user = userMapper.findActiveByUserNo(userNo);
		if (user == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		return user;
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
