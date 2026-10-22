package me._hanho.ultary.domain.neighbor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.neighbor.dto.response.NeighborListItemResponse;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ai.moderation.worker-enabled=false")
@Transactional
class NeighborStoryIntegrationTest {
    @Autowired NeighborService neighbors;
    @Autowired StoryService stories;
    @Autowired PrivacyService privacy;
    @Autowired JdbcTemplate jdbc;
    @Autowired SqlSessionTemplate sqlSession;
    Long viewer, listOwner, author, file;
    UserPrincipal principal;

    @BeforeEach void setup() {
        viewer = user(); listOwner = user(); author = user();
        principal = new UserPrincipal(viewer, "story-list-viewer");
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at,followed_back,followed_back_at) VALUES(?,?,?,'ACCEPTED',NOW(),1,NOW())",
                listOwner, author, NeighborService.pairKey(listOwner, author));
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('story-list.jpg','images/story-list.jpg','image/jpeg','jpg','OWNED',?)", author);
        file = id();
        var settings = new UpdatePrivacyRequest();
        settings.setStoryVisibility("PUBLIC");
        privacy.update(new UserPrincipal(author, "author"), settings);
    }

    @Test void bothListsReturnViewerSpecificUnreadStateAndBecomeReadAfterViewingAllStories() {
        Long first = story(), second = story();
        // The list owner's views must not determine the current viewer's rings.
        stories.markViewed(new UserPrincipal(listOwner, "owner"), first);
        stories.markViewed(new UserPrincipal(listOwner, "owner"), second);
        assertState(true, true);
        stories.markViewed(principal, first);
        assertState(true, true);
        stories.markViewed(principal, second);
        assertState(true, false);
        story();
        assertState(true, true);
    }

    @Test void inaccessibleStoriesExposeNeitherAvailabilityNorUnreadState() {
        story();
        var settings = new UpdatePrivacyRequest();
        settings.setStoryVisibility("PRIVATE");
        privacy.update(new UserPrincipal(author, "author"), settings);
        assertState(false, false);
        settings.setStoryVisibility("PUBLIC"); settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(author, "author"), settings);
        assertState(false, false);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at) VALUES(?,?,?,'ACCEPTED',NOW())",
                viewer, author, NeighborService.pairKey(viewer, author));
        sqlSession.clearCache();
        assertState(true, true);
    }

    @Test void missingAndDeletedStoriesHaveNoRing() {
        assertState(false, false);
        Long story = story();
        assertState(true, true);
        stories.delete(new UserPrincipal(author, "author"), story);
        assertState(false, false);
    }

    void assertState(boolean available, boolean unread) {
        for (String type : new String[] {"FOLLOW", "FOLLOWER"}) {
            var rows = neighbors.listNeighbors(principal, listOwner, type, 50);
            assertThat(rows).extracting(NeighborListItemResponse::getUserNo).containsExactly(author);
            assertThat(rows.get(0).isHasStory()).isEqualTo(available);
            assertThat(rows.get(0).isHasUnviewed()).isEqualTo(unread);
        }
    }

    Long story() {
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE',DATE_ADD(NOW(),INTERVAL 1 DAY))", author, file);
        Long storyId = id();
        // JDBC fixtures bypass MyBatis's transaction-local cache.
        sqlSession.clearCache();
        return storyId;
    }
    Long user() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "story" + UUID.randomUUID().toString().substring(0,12));
        return id();
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
