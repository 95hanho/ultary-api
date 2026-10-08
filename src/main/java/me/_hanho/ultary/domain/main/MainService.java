package me._hanho.ultary.domain.main;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.myultary.dto.response.FeedGridItemResponse;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.main.dto.response.MainFeedPageResponse;
import me._hanho.ultary.domain.main.dto.response.PetTagHistoryItemResponse;
import me._hanho.ultary.domain.main.dto.response.PetTagHistoryPageResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchPetItem;
import me._hanho.ultary.domain.main.dto.response.MainSearchResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchTagItem;
import me._hanho.ultary.domain.main.dto.response.MainSearchUserItem;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryItemResponse;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryPageResponse;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.pet.model.PetMention;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.tag.TagService;
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.UserPetTagHistoryMapper;
import me._hanho.ultary.domain.user.UserSearchHistoryMapper;
import me._hanho.ultary.domain.user.model.PetTagHistoryRow;
import me._hanho.ultary.domain.user.model.SearchHistoryRow;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.domain.user.model.UserSearchHistory;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

	private static final int DEFAULT_FEED_LIMIT = 10;
	private static final int MAX_FEED_LIMIT = 20;
	private static final int DEFAULT_SEARCH_LIMIT = 10;
	private static final int MAX_SEARCH_LIMIT = 20;

	private static final int TAG_FEED_DEFAULT_LIMIT = 30;
	private static final int TAG_FEED_MAX_LIMIT = 50;

	private static final int RECENT_SEARCH_LIMIT = 5;
	private static final int RECENT_SEARCH_MORE_LIMIT = 20;
	private static final int RECENT_PET_TAG_LIMIT = 20;

	private final StoryService storyService;
	private final FeedMapper feedMapper;
	private final FeedService feedService;
	private final UserMapper userMapper;
	private final UserBlockMapper userBlockMapper;
	private final UserSearchHistoryMapper userSearchHistoryMapper;
	private final UserPetTagHistoryMapper userPetTagHistoryMapper;
	private final PetMapper petMapper;
	private final PetService petService;
	private final TagService tagService;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public List<StoryOwnerResponse> getStoryOwners(UserPrincipal principal) {
		return storyService.listResidentOwners(principal);
	}

	@Transactional(readOnly = true)
	public List<StoryResponse> getStories(UserPrincipal principal, Long userNo) {
		return storyService.listByUser(principal, userNo);
	}

	@Transactional(readOnly = true)
	public MainFeedPageResponse getFeeds(UserPrincipal principal, Long cursorFeedId, Integer limit) {
		int pageSize = resolveLimit(limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		Feed cursor = null;
		if (cursorFeedId != null) {
			cursor = feedMapper.findActiveByFeedId(cursorFeedId);
			if (cursor == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "커서가 올바르지 않습니다.");
			}
		}
		List<Feed> rows = feedMapper.findMainTimeline(
				principal.getUserNo(),
				cursor == null ? null : cursor.getCreatedAt(),
				cursor == null ? null : cursor.getFeedId(),
				pageSize + 1);
		Long nextCursor = null;
		if (rows.size() > pageSize) {
			rows = List.copyOf(rows.subList(0, pageSize));
			nextCursor = rows.get(rows.size() - 1).getFeedId();
		}
		return MainFeedPageResponse.builder()
				.items(feedService.toResponses(rows, principal.getUserNo()))
				.nextCursorFeedId(nextCursor)
				.build();
	}

	/**
	 * 메인 추천 게시글.
	 * 나중에 추천 알고리즘 추가해야함. 지금은 조회 가능한 전체 피드를 최신순 limit건.
	 */
	@Transactional(readOnly = true)
	public List<FeedResponse> getRecommendedFeeds(UserPrincipal principal, Integer limit) {
		return listVisibleFeeds(principal, limit);
	}

	/**
	 * 검색 화면 추천 게시글.
	 * 나중에 추천 알고리즘 추가해야함. 지금은 조회 가능한 전체 피드를 최신순 limit건.
	 */
	@Transactional(readOnly = true)
	public List<FeedResponse> getSearchRecommendedFeeds(UserPrincipal principal, Integer limit) {
		return listVisibleFeeds(principal, limit);
	}

	private List<FeedResponse> listVisibleFeeds(UserPrincipal principal, Integer limit) {
		int pageSize = resolveLimit(limit, DEFAULT_FEED_LIMIT, MAX_FEED_LIMIT);
		return feedService.toResponses(
				feedMapper.findVisibleFeeds(principal.getUserNo(), pageSize),
				principal.getUserNo());
	}

	/** 검색창을 열었을 때 최근 울타리 5건 */
	@Transactional(readOnly = true)
	public SearchHistoryPageResponse getRecentSearches(UserPrincipal principal) {
		return pageRecentSearches(principal, null, RECENT_SEARCH_LIMIT);
	}

	/** 최근 검색 더보기. 직전 nextCursorHistoryId 기준으로 20건 */
	@Transactional(readOnly = true)
	public SearchHistoryPageResponse getMoreRecentSearches(UserPrincipal principal, Long cursorHistoryId) {
		if (cursorHistoryId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "cursorHistoryId는 필수입니다.");
		}
		return pageRecentSearches(principal, cursorHistoryId, RECENT_SEARCH_MORE_LIMIT);
	}

	/** 검색 후 그 유저 울타리에 들어갈 때 저장. 같은 울타리는 searched_at만 갱신 */
	@Transactional
	public SearchHistoryItemResponse saveRecentSearch(UserPrincipal principal, Long targetUserNo) {
		if (targetUserNo == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "targetUserNo는 필수입니다.");
		}
		User target = userMapper.findActiveByUserNo(targetUserNo);
		if (target == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
		if (userBlockMapper.findActiveEitherWay(principal.getUserNo(), targetUserNo) != null) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}
		userSearchHistoryMapper.upsert(principal.getUserNo(), targetUserNo);
		SearchHistoryRow row = userSearchHistoryMapper.findByPair(principal.getUserNo(), targetUserNo);
		log.info("[saveRecentSearch] userNo={} targetUserNo={}", principal.getUserNo(), targetUserNo);
		return toHistoryItem(
				row,
				fileService.findSummary(row.getProfileFileId()),
				petTagsByUser(List.of(row.getTargetUserNo())).getOrDefault(row.getTargetUserNo(), List.of()));
	}

	/** 스토리 @·사진 태그 모달의 최근 펫. 검색 최근 울타리와 별도 */
	@Transactional(readOnly = true)
	public PetTagHistoryPageResponse getRecentPetTags(UserPrincipal principal) {
		List<PetTagHistoryRow> rows = userPetTagHistoryMapper.findRecent(
				principal.getUserNo(), RECENT_PET_TAG_LIMIT);
		return PetTagHistoryPageResponse.builder()
				.items(toPetTagItems(rows))
				.build();
	}

	/** 멘션으로 펫을 고를 때 저장. 같은 펫은 used_at만 갱신 */
	@Transactional
	public PetTagHistoryItemResponse saveRecentPetTag(UserPrincipal principal, Long petId) {
		if (petId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "petId는 필수입니다.");
		}
		Pet pet = petMapper.findActiveByPetId(petId);
		if (pet == null) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		User owner = userMapper.findActiveByUserNo(pet.getUserNo());
		if (owner == null) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		if (!principal.getUserNo().equals(pet.getUserNo())
				&& userBlockMapper.findActiveEitherWay(principal.getUserNo(), pet.getUserNo()) != null) {
			throw new BusinessException(ErrorCode.USER_BLOCKED);
		}
		userPetTagHistoryMapper.upsert(principal.getUserNo(), petId);
		PetTagHistoryRow row = userPetTagHistoryMapper.findByPair(principal.getUserNo(), petId);
		log.info("[saveRecentPetTag] userNo={} petId={}", principal.getUserNo(), petId);
		return toPetTagItem(row, fileService.findSummary(row.getProfileFileId()));
	}

	/** 내 최근 펫 태그 전부 삭제. 목록에 안 보이던 행도 포함 */
	@Transactional
	public void clearRecentPetTags(UserPrincipal principal) {
		int deleted = userPetTagHistoryMapper.deleteByUserNo(principal.getUserNo());
		log.info("[clearRecentPetTags] userNo={} deleted={}", principal.getUserNo(), deleted);
	}

	/** 최근 검색 한 건. 내 행만 지운다 */
	@Transactional
	public void deleteRecentSearch(UserPrincipal principal, Long userSearchHistoryId) {
		int deleted = userSearchHistoryMapper.deleteOwnById(principal.getUserNo(), userSearchHistoryId);
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "최근 검색을 찾을 수 없습니다.");
		}
		log.info("[deleteRecentSearch] userNo={} userSearchHistoryId={}",
				principal.getUserNo(), userSearchHistoryId);
	}

	/** 모두 지우기. 목록에 안 보이는 탈퇴·차단 대상 행도 함께 삭제 */
	@Transactional
	public void clearRecentSearches(UserPrincipal principal) {
		int deleted = userSearchHistoryMapper.deleteByUserNo(principal.getUserNo());
		log.info("[clearRecentSearches] userNo={} deleted={}", principal.getUserNo(), deleted);
	}

	private SearchHistoryPageResponse pageRecentSearches(
			UserPrincipal principal, Long cursorHistoryId, int pageSize) {
		LocalDateTime cursorSearchedAt = null;
		Long cursorId = null;
		if (cursorHistoryId != null) {
			UserSearchHistory cursor = userSearchHistoryMapper.findOwnById(
					principal.getUserNo(), cursorHistoryId);
			if (cursor == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "커서가 올바르지 않습니다.");
			}
			cursorSearchedAt = cursor.getSearchedAt();
			cursorId = cursor.getUserSearchHistoryId();
		}
		List<SearchHistoryRow> rows = userSearchHistoryMapper.findRecent(
				principal.getUserNo(), cursorSearchedAt, cursorId, pageSize + 1);
		Long nextCursor = null;
		if (rows.size() > pageSize) {
			rows = List.copyOf(rows.subList(0, pageSize));
			nextCursor = rows.get(rows.size() - 1).getUserSearchHistoryId();
		}
		Set<Long> profileIds = new HashSet<>();
		for (SearchHistoryRow row : rows) {
			if (row.getProfileFileId() != null) {
				profileIds.add(row.getProfileFileId().longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		Map<Long, List<String>> petTags = petTagsByUser(
				rows.stream().map(SearchHistoryRow::getTargetUserNo).toList());
		List<SearchHistoryItemResponse> items = rows.stream()
				.map(row -> toHistoryItem(
						row,
						row.getProfileFileId() == null
								? null
								: files.get(row.getProfileFileId().longValue()),
						petTags.getOrDefault(row.getTargetUserNo(), List.of())))
				.toList();
		return SearchHistoryPageResponse.builder()
				.items(items)
				.nextCursorHistoryId(nextCursor)
				.build();
	}

	private Map<Long, String> ownerNicknames(List<Long> userNos) {
		if (userNos == null || userNos.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = userNos.stream().distinct().toList();
		Map<Long, String> nicknames = new HashMap<>();
		for (User user : userMapper.findActiveByUserNos(ids)) {
			nicknames.put(user.getUserNo(), user.getNickname());
		}
		return nicknames;
	}

	private List<PetTagHistoryItemResponse> toPetTagItems(List<PetTagHistoryRow> rows) {
		Set<Long> profileIds = new HashSet<>();
		for (PetTagHistoryRow row : rows) {
			if (row.getProfileFileId() != null) {
				profileIds.add(row.getProfileFileId());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		return rows.stream()
				.map(row -> toPetTagItem(
						row,
						row.getProfileFileId() == null ? null : files.get(row.getProfileFileId())))
				.toList();
	}

	private PetTagHistoryItemResponse toPetTagItem(PetTagHistoryRow row, FileSummaryResponse profileFile) {
		Long profileFileId = row.getProfileFileId();
		return PetTagHistoryItemResponse.builder()
				.petId(row.getPetId())
				.mentionId(row.getMentionId())
				.name(row.getName())
				.userNo(row.getOwnerUserNo())
				.ownerNickname(row.getOwnerNickname())
				.profileFileId(profileFileId)
				.profileFile(profileFile)
				.usedAt(row.getUsedAt())
				.build();
	}

	private Map<Long, List<String>> petTagsByUser(List<Long> userNos) {
		if (userNos == null || userNos.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = userNos.stream().distinct().toList();
		Map<Long, List<String>> tags = new HashMap<>();
		for (PetMention mention : petMapper.findActiveMentions(ids)) {
			tags.computeIfAbsent(mention.getUserNo(), key -> new ArrayList<>())
					.add(mention.getMentionId());
		}
		return tags;
	}

	private SearchHistoryItemResponse toHistoryItem(
			SearchHistoryRow row, FileSummaryResponse profileFile, List<String> petTags) {
		return SearchHistoryItemResponse.builder()
				.userSearchHistoryId(row.getUserSearchHistoryId())
				.userNo(row.getTargetUserNo())
				.nickname(row.getNickname())
				.profileFileId(row.getProfileFileId())
				.profileFile(profileFile)
				.petTags(petTags == null ? List.of() : petTags)
				.searchedAt(row.getSearchedAt())
				.build();
	}

	@Transactional(readOnly = true)
	public MainSearchResponse search(UserPrincipal principal, String q, String type, Integer limit) {
		String query = normalizeQuery(q);
		if (!StringUtils.hasText(query)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어를 입력해 주세요.");
		}
		int resolved = resolveLimit(limit, DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT);
		String searchType = normalizeSearchType(type);
		boolean all = "ALL".equals(searchType);

		List<MainSearchUserItem> users = Collections.emptyList();
		List<MainSearchPetItem> pets = Collections.emptyList();
		List<MainSearchTagItem> tags = Collections.emptyList();
		List<FeedResponse> feeds = Collections.emptyList();

		if (all || "USER".equals(searchType)) {
			users = toSearchUsers(userMapper.searchActiveByNickname(
					principal.getUserNo(), query, resolved));
		}
		if ("MENTION".equals(searchType)) {
			users = toSearchUsers(userMapper.searchActiveByPetMention(
					principal.getUserNo(), query, resolved));
		}
		if (all || "PET".equals(searchType)) {
			List<Pet> petRows = petMapper.searchActive(principal.getUserNo(), query, resolved);
			Set<Long> profileIds = new HashSet<>();
			for (Pet p : petRows) {
				if (p.getProfileFileId() != null) {
					profileIds.add(p.getProfileFileId());
				}
			}
			Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
			Map<Long, String> ownerNicknames = ownerNicknames(petRows.stream().map(Pet::getUserNo).toList());
			pets = petRows.stream()
					.map(p -> MainSearchPetItem.builder()
							.petId(p.getPetId())
							.userNo(p.getUserNo())
							.ownerNickname(ownerNicknames.get(p.getUserNo()))
							.mentionId(p.getMentionId())
							.name(p.getName())
							.species(p.getSpecies())
							.profileFileId(p.getProfileFileId())
							.profileFile(p.getProfileFileId() == null
									? null
									: files.get(p.getProfileFileId()))
							.build())
					.toList();
		}
		if (all || "TAG".equals(searchType)) {
			tags = tagService.searchByHashtag(principal.getUserNo(), query, resolved).stream()
					.map(hit -> MainSearchTagItem.builder()
							.tagId(hit.getTagId())
							.hashtag(hit.getHashtag())
							.feedCount(hit.getFeedCount() == null ? 0 : hit.getFeedCount())
							.build())
					.toList();
		}
		if (all || "FEED".equals(searchType)) {
			feeds = feedService.toResponses(
					feedMapper.searchVisible(principal.getUserNo(), query, resolved),
					principal.getUserNo());
		}

		return MainSearchResponse.builder()
				.users(users)
				.pets(pets)
				.tags(tags)
				.feeds(feeds)
				.build();
	}

	/** 태그명 목록에서 고른 태그의 게시글 그리드. 조회자에게 보이는 글만, 최신순 */
	@Transactional(readOnly = true)
	public List<FeedGridItemResponse> listFeedsByTag(UserPrincipal principal, Long tagId, Integer limit) {
		tagService.ensureActive(tagId);
		int pageSize = resolveLimit(limit, TAG_FEED_DEFAULT_LIMIT, TAG_FEED_MAX_LIMIT);
		return toGridItems(feedMapper.findVisibleByTag(principal.getUserNo(), tagId, pageSize));
	}

	private List<MainSearchUserItem> toSearchUsers(List<User> userRows) {
		if (userRows == null || userRows.isEmpty()) {
			return List.of();
		}
		Map<Long, Integer> profileByUser = petService.representativeProfileFileIds(
				userRows.stream().map(User::getUserNo).toList());
		Set<Long> profileIds = new HashSet<>();
		for (Integer profileFileId : profileByUser.values()) {
			profileIds.add(profileFileId.longValue());
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		Map<Long, List<String>> petTags = petTagsByUser(
				userRows.stream().map(User::getUserNo).toList());
		return userRows.stream()
				.map(u -> {
					Integer profileFileId = profileByUser.get(u.getUserNo());
					Long profileId = profileFileId == null ? null : profileFileId.longValue();
					return MainSearchUserItem.builder()
							.userNo(u.getUserNo())
							.nickname(u.getNickname())
							.profileFileId(profileFileId)
							.profileFile(profileId == null ? null : files.get(profileId))
							.petTags(petTags.getOrDefault(u.getUserNo(), List.of()))
							.bio(u.getBio())
							.build();
				})
				.toList();
	}

	private List<FeedGridItemResponse> toGridItems(List<Feed> feeds) {
		if (feeds == null || feeds.isEmpty()) {
			return List.of();
		}
		List<Long> feedIds = feeds.stream().map(Feed::getFeedId).toList();
		Map<Long, List<FeedMedia>> mediaByFeed = new HashMap<>();
		for (FeedMedia media : feedMapper.findMediaByFeedIds(feedIds)) {
			mediaByFeed.computeIfAbsent(media.getFeedId(), key -> new ArrayList<>()).add(media);
		}
		Set<Long> fileIds = new HashSet<>();
		for (List<FeedMedia> mediaList : mediaByFeed.values()) {
			FeedMedia cover = mediaList.get(0);
			if (cover.getFileId() != null) {
				fileIds.add(cover.getFileId());
			}
			if (cover.getThumbnailFileId() != null) {
				fileIds.add(cover.getThumbnailFileId());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		List<FeedGridItemResponse> items = new ArrayList<>();
		for (Feed feed : feeds) {
			List<FeedMedia> mediaList = mediaByFeed.getOrDefault(feed.getFeedId(), List.of());
			FeedMedia cover = mediaList.isEmpty() ? null : mediaList.get(0);
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
					.mediaCount(mediaList.size())
					.likeCount(feed.getLikeCount())
					.commentCount(feed.getCommentCount())
					.createdAt(feed.getCreatedAt())
					.build());
		}
		return items;
	}

	private String normalizeQuery(String q) {
		if (!StringUtils.hasText(q)) {
			return "";
		}
		String value = q.trim();
		if (value.startsWith("@") || value.startsWith("#")) {
			value = value.substring(1).trim();
		}
		return value;
	}

	private String normalizeSearchType(String type) {
		if (!StringUtils.hasText(type)) {
			return "ALL";
		}
		String value = type.trim().toUpperCase(Locale.ROOT);
		if ("NICKNAME".equals(value) || "USERS".equals(value)) {
			return "USER";
		}
		if ("PETS".equals(value)) {
			return "PET";
		}
		if ("MENTION".equals(value) || "PET_MENTION".equals(value)) {
			return "MENTION";
		}
		if ("TAGS".equals(value) || "HASHTAG".equals(value)) {
			return "TAG";
		}
		if ("FEEDS".equals(value) || "POST".equals(value)) {
			return "FEED";
		}
		if (!List.of("ALL", "USER", "MENTION", "PET", "TAG", "FEED").contains(value)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 ALL, USER, MENTION, PET, TAG, FEED 입니다.");
		}
		return value;
	}

	private int resolveLimit(Integer limit, int defaultLimit, int maxLimit) {
		if (limit == null || limit < 1) {
			return defaultLimit;
		}
		return Math.min(limit, maxLimit);
	}
}
