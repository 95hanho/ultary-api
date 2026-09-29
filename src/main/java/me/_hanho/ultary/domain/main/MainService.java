package me._hanho.ultary.domain.main;

import java.time.LocalDateTime;
import java.util.Collections;
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
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.main.dto.response.MainFeedPageResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchPetItem;
import me._hanho.ultary.domain.main.dto.response.MainSearchResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchUserItem;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryItemResponse;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryPageResponse;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.tag.TagService;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.UserSearchHistoryMapper;
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

	private static final int RECENT_SEARCH_LIMIT = 5;
	private static final int RECENT_SEARCH_MORE_LIMIT = 20;

	private final StoryService storyService;
	private final FeedMapper feedMapper;
	private final FeedService feedService;
	private final UserMapper userMapper;
	private final UserBlockMapper userBlockMapper;
	private final UserSearchHistoryMapper userSearchHistoryMapper;
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
		return toHistoryItem(row, fileService.findSummary(row.getProfileFileId()));
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
		List<SearchHistoryItemResponse> items = rows.stream()
				.map(row -> toHistoryItem(
						row,
						row.getProfileFileId() == null
								? null
								: files.get(row.getProfileFileId().longValue())))
				.toList();
		return SearchHistoryPageResponse.builder()
				.items(items)
				.nextCursorHistoryId(nextCursor)
				.build();
	}

	private SearchHistoryItemResponse toHistoryItem(SearchHistoryRow row, FileSummaryResponse profileFile) {
		return SearchHistoryItemResponse.builder()
				.userSearchHistoryId(row.getUserSearchHistoryId())
				.userNo(row.getTargetUserNo())
				.nickname(row.getNickname())
				.profileFileId(row.getProfileFileId())
				.profileFile(profileFile)
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
		List<TagResponse> tags = Collections.emptyList();
		List<FeedResponse> feeds = Collections.emptyList();

		if (all || "USER".equals(searchType)) {
			List<User> userRows = userMapper.searchActiveByNickname(principal.getUserNo(), query, resolved);
			Map<Long, Integer> profileByUser = petService.representativeProfileFileIds(
					userRows.stream().map(User::getUserNo).toList());
			Set<Long> profileIds = new HashSet<>();
			for (Integer profileFileId : profileByUser.values()) {
				profileIds.add(profileFileId.longValue());
			}
			Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
			users = userRows.stream()
					.map(u -> {
						Integer profileFileId = profileByUser.get(u.getUserNo());
						Long profileId = profileFileId == null ? null : profileFileId.longValue();
						return MainSearchUserItem.builder()
								.userNo(u.getUserNo())
								.nickname(u.getNickname())
								.profileFileId(profileFileId)
								.profileFile(profileId == null ? null : files.get(profileId))
								.bio(u.getBio())
								.build();
					})
					.toList();
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
			pets = petRows.stream()
					.map(p -> MainSearchPetItem.builder()
							.petId(p.getPetId())
							.userNo(p.getUserNo())
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
			tags = tagService.search(query, resolved);
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
		if ("TAGS".equals(value) || "HASHTAG".equals(value)) {
			return "TAG";
		}
		if ("FEEDS".equals(value) || "POST".equals(value)) {
			return "FEED";
		}
		if (!List.of("ALL", "USER", "PET", "TAG", "FEED").contains(value)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 ALL, USER, PET, TAG, FEED 입니다.");
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
