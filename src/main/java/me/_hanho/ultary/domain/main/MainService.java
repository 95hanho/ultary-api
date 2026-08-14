package me._hanho.ultary.domain.main;

import java.util.Collections;
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
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.main.dto.response.MainFeedPageResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchPetItem;
import me._hanho.ultary.domain.main.dto.response.MainSearchResponse;
import me._hanho.ultary.domain.main.dto.response.MainSearchUserItem;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.tag.TagService;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainService {

	private static final int DEFAULT_FEED_LIMIT = 10;
	private static final int MAX_FEED_LIMIT = 20;
	private static final int DEFAULT_SEARCH_LIMIT = 10;
	private static final int MAX_SEARCH_LIMIT = 20;

	private final StoryService storyService;
	private final FeedMapper feedMapper;
	private final FeedService feedService;
	private final UserMapper userMapper;
	private final PetMapper petMapper;
	private final TagService tagService;

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
			users = userMapper.searchActiveByNickname(principal.getUserNo(), query, resolved).stream()
					.map(u -> MainSearchUserItem.builder()
							.userNo(u.getUserNo())
							.nickname(u.getNickname())
							.profileFileId(u.getProfileFileId())
							.bio(u.getBio())
							.build())
					.toList();
		}
		if (all || "PET".equals(searchType)) {
			pets = petMapper.searchActive(principal.getUserNo(), query, resolved).stream()
					.map(p -> MainSearchPetItem.builder()
							.petId(p.getPetId())
							.userNo(p.getUserNo())
							.mentionId(p.getMentionId())
							.name(p.getName())
							.species(p.getSpecies())
							.profileFileId(p.getProfileFileId())
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
