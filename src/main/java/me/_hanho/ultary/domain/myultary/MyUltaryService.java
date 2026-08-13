package me._hanho.ultary.domain.myultary;

import java.util.ArrayList;
import java.util.List;

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
import me._hanho.ultary.domain.myultary.dto.request.UpdateMyBioRequest;
import me._hanho.ultary.domain.myultary.dto.request.UpdateMyProfileImageRequest;
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.myultary.dto.response.MyUltaryProfileResponse;
import me._hanho.ultary.domain.neighbor.NeighborMapper;
import me._hanho.ultary.domain.pet.PetMapper;
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
	private final FeedMapper feedMapper;
	private final FeedService feedService;
	private final FileService fileService;
	private final StoryService storyService;

	@Transactional(readOnly = true)
	public MyUltaryProfileResponse getProfile(UserPrincipal principal) {
		User user = requireUser(principal.getUserNo());
		return MyUltaryProfileResponse.builder()
				.userNo(user.getUserNo())
				.nickname(user.getNickname())
				.defaultNickname(Boolean.TRUE.equals(user.getIsDefaultNickname()))
				.profileFileId(user.getProfileFileId())
				.bio(user.getBio())
				.regionSido(user.getRegionSido())
				.regionSigungu(user.getRegionSigungu())
				.hasStory(storyService.hasActiveStory(user.getUserNo()))
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
	public MyUltaryProfileResponse updateProfileImage(
			UserPrincipal principal, UpdateMyProfileImageRequest request) {
		boolean remove = Boolean.TRUE.equals(request.getRemoveProfileFile());
		if (!remove && request.getProfileFileId() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "profileFileId 또는 removeProfileFile이 필요합니다.");
		}
		if (!remove) {
			fileService.requireActive(request.getProfileFileId());
		}
		int updated = userMapper.updateProfileFileId(
				principal.getUserNo(),
				request.getProfileFileId(),
				remove);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		log.info("[updateProfileImage] userNo={} remove={}", principal.getUserNo(), remove);
		return getProfile(principal);
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
		List<FeedGridItemResponse> items = new ArrayList<>();
		for (Feed feed : feeds) {
			List<FeedMedia> mediaList = feedMapper.findMediaByFeedId(feed.getFeedId());
			FeedMedia cover = mediaList.isEmpty() ? null : mediaList.get(0);
			items.add(FeedGridItemResponse.builder()
					.feedId(feed.getFeedId())
					.coverFileId(cover != null ? cover.getFileId() : null)
					.coverThumbnailFileId(cover != null ? cover.getThumbnailFileId() : null)
					.coverMediaType(cover != null ? cover.getMediaType() : null)
					.mediaCount(mediaList.size())
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
