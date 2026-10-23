package me._hanho.ultary.domain.neighbor;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.main.MainService;
import me._hanho.ultary.domain.story.StoryService;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class DirectionalBlockIntegrationTest {
    @Autowired NeighborService neighbors;
    @Autowired UserMapper users;
    @Autowired FeedService feeds;
    @Autowired StoryService stories;
    @Autowired MainService main;
    @Autowired JdbcTemplate jdbc;
    Long blocker, blocked, other, blockerFeed, otherFeed;
    String blockerNick, blockedNick;
    UserPrincipal a, b;

    @BeforeEach void setup() {
        blockerNick = nick(); blockedNick = nick();
        blocker = user(blockerNick); blocked = user(blockedNick); other = user(nick());
        a = new UserPrincipal(blocker, blockerNick); b = new UserPrincipal(blocked, blockedNick);
        blockerFeed = feed(blocker); otherFeed = feed(other);
        neighbors.block(a, blocked);
    }

    @Test void blockerCanSearchAndReadBasicProfileButBlockedCannotFindBlocker() {
        assertThat(users.searchActiveByNickname(blocker, blockedNick, 20)).extracting(x -> x.getUserNo()).contains(blocked);
        assertThat(users.searchActiveByNickname(blocked, blockerNick, 20)).isEmpty();
        assertThat(neighbors.getUltary(a, blocked).isBlockedByMe()).isTrue();
        assertThatThrownBy(() -> neighbors.getUltary(b, blocker)).satisfies(error -> missing(error, ErrorCode.USER_NOT_FOUND));
        assertThatThrownBy(() -> neighbors.assertCanViewUltary(b, blocker)).satisfies(error -> missing(error, ErrorCode.USER_NOT_FOUND));
        main.saveRecentSearch(a, userSearch(blocked));
        assertThat(main.getRecentSearches(a).getItems()).extracting(x -> x.getUserNo()).contains(blocked);
    }

    @Test void blockedDirectFeedAndStoryAccessLooksMissingAndGuestPublicFeedStillWorks() {
        assertThatThrownBy(() -> feeds.getDetail(b, blockerFeed)).satisfies(error -> missing(error, ErrorCode.FEED_NOT_FOUND));
        assertThat(feeds.getDetail(null, blockerFeed).getFeedId()).isEqualTo(blockerFeed);
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('block.jpg','images/block.jpg','image/jpeg','jpg','OWNED',?)", blocker);
        Long file = id();
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE','2099-12-31 23:59:59')", blocker, file);
        Long story = id();
        assertThat(stories.hasVisibleStory(blocked, blocker)).isFalse();
        assertThatThrownBy(() -> stories.listByUser(b, blocker)).satisfies(error -> missing(error, ErrorCode.STORY_NOT_FOUND));
        assertThatThrownBy(() -> stories.markViewed(b, story)).satisfies(error -> missing(error, ErrorCode.STORY_NOT_FOUND));
    }

    @Test void commentsAndBothInlineAndExpandedRepliesExcludeBlockingAuthors() {
        Long hidden = comment(blocker), visible = comment(other);
        reply(blocker, visible); Long visibleReply = reply(other, visible);
        var comments = feeds.getComments(b, otherFeed, 50);
        assertThat(comments).extracting(x -> x.getFeedCommentId()).containsExactly(visible);
        assertThat(comments.get(0).getReplies()).extracting(x -> x.getFeedReplyId()).containsExactly(visibleReply);
        assertThat(feeds.getReplies(b, otherFeed, visible, 50)).extracting(x -> x.getFeedReplyId()).containsExactly(visibleReply);
        assertThatThrownBy(() -> feeds.getReplies(b, otherFeed, hidden, 50)).satisfies(error -> missing(error, ErrorCode.FEED_COMMENT_NOT_FOUND));
    }

    me._hanho.ultary.domain.main.dto.request.SaveSearchHistoryRequest userSearch(Long user) {
        var request = new me._hanho.ultary.domain.main.dto.request.SaveSearchHistoryRequest();
        request.setTargetUserNo(user); return request;
    }
    void missing(Throwable error, ErrorCode code) {
        assertThat(error).isInstanceOfSatisfying(BusinessException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(code);
            assertThat(ex.getErrorCode().getHttpStatus().value()).isEqualTo(404);
        });
    }
    Long comment(Long user) { jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'comment')", otherFeed, user); return id(); }
    Long reply(Long user, Long comment) { jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'reply')", comment, user); return id(); }
    Long feed(Long user) { jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,'PUBLIC')", user); return id(); }
    Long user(String nickname) { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", nickname); return id(); }
    String nick() { return "dir_" + UUID.randomUUID().toString().substring(0, 10); }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
