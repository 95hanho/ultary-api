package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ai.moderation.worker-enabled=false")
@Transactional
class FeedAuthorStoryIntegrationTest {
    @Autowired FeedService feeds;
    @Autowired StoryService stories;
    @Autowired PrivacyService privacy;
    @Autowired JdbcTemplate jdbc;
    @Test void feedCommentAndReplyReturnUnreadReadAndUnavailableStoryFlags() {
        Long author = user(), viewer = user(); var me = new UserPrincipal(viewer, "viewer");
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,'PUBLIC')", author); Long feed = id();
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'comment')", feed, author); Long comment = id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'reply')", comment, author);
        assertThat(feeds.getDetail(me, feed).isAuthorHasStory()).isFalse();
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('story.jpg','images/story.jpg','image/jpeg','jpg','OWNED',?)", author); Long file = id();
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE','2099-12-31 23:59:59')", author, file); Long story = id();
        var setting = new UpdatePrivacyRequest(); setting.setStoryVisibility("PUBLIC"); privacy.update(new UserPrincipal(author, "author"), setting);
        assertThat(feeds.getDetail(me, feed).isAuthorHasUnviewedStory()).isTrue();
        assertThat(feeds.getComments(me, feed, 20).get(0).isAuthorHasUnviewedStory()).isTrue();
        assertThat(feeds.getReplies(me, feed, comment, 20).get(0).isAuthorHasUnviewedStory()).isTrue();
        stories.markViewed(me, story);
        assertThat(feeds.getDetail(me, feed).isAuthorHasStory()).isTrue();
        assertThat(feeds.getDetail(me, feed).isAuthorHasUnviewedStory()).isFalse();
        assertThat(feeds.getComments(me, feed, 20).get(0).isAuthorHasUnviewedStory()).isFalse();
        assertThat(feeds.getReplies(me, feed, comment, 20).get(0).isAuthorHasUnviewedStory()).isFalse();
        setting.setStoryVisibility("PRIVATE"); privacy.update(new UserPrincipal(author, "author"), setting);
        assertThat(feeds.getDetail(me, feed).isAuthorHasStory()).isFalse();
    }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "story_" + UUID.randomUUID().toString().substring(0, 9)); return id(); }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
