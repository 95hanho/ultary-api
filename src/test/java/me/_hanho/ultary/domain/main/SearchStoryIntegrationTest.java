package me._hanho.ultary.domain.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.main.dto.request.SaveSearchHistoryRequest;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.user.UserSearchHistoryMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ai.moderation.worker-enabled=false")
@Transactional
class SearchStoryIntegrationTest {
    @Autowired MainService main;
    @Autowired StoryService stories;
    @Autowired PrivacyService privacy;
    @Autowired UserSearchHistoryMapper history;
    @Autowired JdbcTemplate jdbc;
    Long viewer, author, story;
    String nickname, mention;
    UserPrincipal principal;

    @BeforeEach void setup() {
        viewer = user(); author = user(); principal = new UserPrincipal(viewer, "search-story");
        nickname = jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?", String.class, author);
        mention = "storypet" + author;
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,1)", author, mention, "search pet");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('search-story.jpg','images/search-story.jpg','image/jpeg','jpg','OWNED',?)", author);
        Long file = id();
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE',DATE_ADD(NOW(),INTERVAL 1 DAY))", author, file);
        story = id();
        var settings = new UpdatePrivacyRequest(); settings.setStoryVisibility("PUBLIC");
        privacy.update(new UserPrincipal(author, "author"), settings);
    }

    @Test void nicknamePetAndRecentSearchesReturnTheCurrentViewersReadState() {
        stories.markViewed(new UserPrincipal(author, "author"), story);
        assertAllChannels(true, true);
        stories.markViewed(principal, story);
        assertAllChannels(true, false);
    }

    @Test void privateAndMissingStoriesDoNotExposeStoryRings() {
        var settings = new UpdatePrivacyRequest(); settings.setStoryVisibility("PRIVATE");
        privacy.update(new UserPrincipal(author, "author"), settings);
        assertAllChannels(false, false);
        settings.setStoryVisibility("PUBLIC"); settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(author, "author"), settings);
        assertAllChannels(false, false);
        stories.delete(new UserPrincipal(author, "author"), story);
        settings.setPrivateAccount(false);
        privacy.update(new UserPrincipal(author, "author"), settings);
        assertAllChannels(false, false);
    }

    @Test void recentMoreIncludesStoryStateAndTagRowsHaveNoStory() {
        history.upsertUser(viewer, author);
        for (int i = 0; i < 4; i++) history.upsertUser(viewer, user());
        jdbc.update("INSERT INTO ultary_tag(hashtag,created_by_user_no) VALUES(?,?)", "storytag" + author, viewer);
        history.upsertTag(viewer, id());
        var first = main.getRecentSearches(principal);
        assertThat(first.getItems()).hasSize(5).allSatisfy(item -> {
            assertThat(item.isHasStory()).isFalse();
            assertThat(item.isHasUnviewed()).isFalse();
        });
        var more = main.getMoreRecentSearches(principal, first.getNextCursorHistoryId());
        assertThat(more.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getUserNo()).isEqualTo(author);
            assertThat(item.isHasStory()).isTrue();
            assertThat(item.isHasUnviewed()).isTrue();
        });
    }

    void assertAllChannels(boolean available, boolean unread) {
        for (String type : new String[] {"USER", "MENTION"}) {
            var results = main.search(principal, type.equals("USER") ? nickname : mention, type, 20).getUsers();
            assertThat(results).singleElement().satisfies(item -> {
                assertThat(item.getUserNo()).isEqualTo(author);
                assertThat(item.isHasStory()).isEqualTo(available);
                assertThat(item.isHasUnviewed()).isEqualTo(unread);
            });
        }
        var save = new SaveSearchHistoryRequest(); save.setTargetUserNo(author);
        var saved = main.saveRecentSearch(principal, save);
        assertThat(saved.isHasStory()).isEqualTo(available);
        assertThat(saved.isHasUnviewed()).isEqualTo(unread);
        assertThat(main.getRecentSearches(principal).getItems()).singleElement().satisfies(item -> {
            assertThat(item.isHasStory()).isEqualTo(available);
            assertThat(item.isHasUnviewed()).isEqualTo(unread);
        });
    }

    Long user() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "srch" + UUID.randomUUID().toString().replace("-", "").substring(0,12));
        return id();
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
