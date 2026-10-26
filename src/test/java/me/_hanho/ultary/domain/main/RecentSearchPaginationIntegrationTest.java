package me._hanho.ultary.domain.main;

import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.main.dto.response.SearchHistoryItemResponse;
import me._hanho.ultary.domain.user.UserSearchHistoryMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class RecentSearchPaginationIntegrationTest {
    @Autowired MainService service;
    @Autowired UserSearchHistoryMapper mapper;
    @Autowired JdbcTemplate jdbc;
    Long me;
    UserPrincipal principal;

    @BeforeEach void setup() { me = user(); principal = new UserPrincipal(me, "recent-test"); }

    @Test void returnsFiveThenTwentyUntilExhaustedWithTheSameVisibleTotal() {
        for (int i = 0; i < 47; i++) {
            if (i % 2 == 0) mapper.upsertUser(me, user()); else mapper.upsertTag(me, tag(me));
        }
        // Identical times must still paginate without duplicates or omissions.
        jdbc.update("UPDATE ultary_user_search_history SET searched_at='2026-10-01 12:00:00' WHERE user_no=?", me);
        var page = service.getRecentSearches(principal);
        assertThat(page.getItems()).hasSize(5);
        assertThat(page.getTotalCount()).isEqualTo(47);
        List<Long> ids = new ArrayList<>();
        ids.addAll(page.getItems().stream().map(SearchHistoryItemResponse::getUserSearchHistoryId).toList());
        for (int size : List.of(20, 20, 2)) {
            assertThat(page.getNextCursorHistoryId()).isNotNull();
            page = service.getMoreRecentSearches(principal, page.getNextCursorHistoryId());
            assertThat(page.getItems()).hasSize(size);
            assertThat(page.getTotalCount()).isEqualTo(47);
            ids.addAll(page.getItems().stream().map(SearchHistoryItemResponse::getUserSearchHistoryId).toList());
        }
        assertThat(page.getNextCursorHistoryId()).isNull();
        assertThat(ids).hasSize(47).doesNotHaveDuplicates().isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test void totalUsesTheSameFiltersAsTheListAndStaysScopedToTheViewer() {
        Long visibleUser = user(), visibleTag = tag(me);
        mapper.upsertUser(me, visibleUser); mapper.upsertTag(me, visibleTag);
        Long withdrawn = user(); mapper.upsertUser(me, withdrawn);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='WITHDRAWN' WHERE user_no=?", withdrawn);
        Long blocked = user(), blocker = user(); mapper.upsertUser(me, blocked); mapper.upsertUser(me, blocker);
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?),(?,?)", me,blocked,blocker,me);
        Long deletedTag = tag(me); mapper.upsertTag(me, deletedTag);
        jdbc.update("UPDATE ultary_tag SET is_deleted=1 WHERE tag_id=?", deletedTag);
        Long suspended = user(); mapper.upsertTag(me, tag(suspended));
        jdbc.update("UPDATE ultary_user SET withdrawal_status='SUSPENDED' WHERE user_no=?", suspended);
        mapper.upsertUser(user(), user());
        var page = service.getRecentSearches(principal);
        // A user I blocked remains searchable; a user who blocked me is hidden.
        assertThat(page.getTotalCount()).isEqualTo(3);
        assertThat(page.getItems()).hasSize(3);
        assertThat(page.getItems()).filteredOn(item -> "USER".equals(item.getType()))
            .extracting(SearchHistoryItemResponse::getUserNo).containsExactlyInAnyOrder(visibleUser, blocked);
        assertThat(page.getItems()).filteredOn(item -> "TAG".equals(item.getType()))
            .extracting(SearchHistoryItemResponse::getTagId).containsExactly(visibleTag);
        assertThat(page.getNextCursorHistoryId()).isNull();
    }

    @Test void deletionUpdatesTotalAndCanContinueFromThePreviousVisibleItem() {
        for (int i = 0; i < 8; i++) mapper.upsertUser(me, user());
        var first = service.getRecentSearches(principal);
        Long removedCursor = first.getNextCursorHistoryId();
        Long preceding = first.getItems().get(3).getUserSearchHistoryId();
        service.deleteRecentSearch(principal, removedCursor);
        var more = service.getMoreRecentSearches(principal, preceding);
        assertThat(more.getItems()).hasSize(3);
        assertThat(more.getTotalCount()).isEqualTo(7);
        assertThat(more.getNextCursorHistoryId()).isNull();
        mapper.deleteByUserNo(me);
        var empty = service.getRecentSearches(principal);
        assertThat(empty.getItems()).isEmpty();
        assertThat(empty.getTotalCount()).isZero();
        assertThat(empty.getNextCursorHistoryId()).isNull();
    }

    @Test void recentTagsReturnOnlyVisiblePostCountsIncludingZero() {
        Long tag = tag(me), emptyTag = tag(me), other = user(), blocked = user();
        mapper.upsertTag(me, tag); mapper.upsertTag(me, emptyTag);
        feed(tag, me, "PRIVATE"); // own private post is visible
        feed(tag, other, "PUBLIC");
        feed(tag, other, "PRIVATE");
        Long deletedFeed = feed(tag, other, "PUBLIC");
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?", deletedFeed);
        Long removedLink = feed(tag, other, "PUBLIC");
        jdbc.update("UPDATE ultary_feed_tag SET is_deleted=1 WHERE feed_id=?", removedLink);
        feed(tag, blocked, "PUBLIC");
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?)", blocked, me);
        var items = service.getRecentSearches(principal).getItems();
        assertThat(items).filteredOn(item -> tag.equals(item.getTagId()))
            .singleElement().extracting(SearchHistoryItemResponse::getFeedCount).isEqualTo(2L);
        assertThat(items).filteredOn(item -> emptyTag.equals(item.getTagId()))
            .singleElement().extracting(SearchHistoryItemResponse::getFeedCount).isEqualTo(0L);
        assertThat(mapper.findByTagPair(me, tag).getFeedCount()).isEqualTo(2L);
    }

    Long feed(Long tag, Long author, String visibility) {
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,?)", author, visibility);
        Long feed = id();
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)", feed, tag);
        return feed;
    }

    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "recent"+UUID.randomUUID().toString().substring(0,12)); return id(); }
    Long tag(Long creator) { jdbc.update("INSERT INTO ultary_tag(hashtag,created_by_user_no) VALUES(?,?)", "recent"+UUID.randomUUID().toString().replace("-", "").substring(0,12),creator); return id(); }
}
