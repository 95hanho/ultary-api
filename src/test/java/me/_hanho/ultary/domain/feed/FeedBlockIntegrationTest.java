package me._hanho.ultary.domain.feed;

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
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class FeedBlockIntegrationTest {
    @Autowired FeedService feeds;
    @Autowired UserBlockMapper blocks;
    @Autowired JdbcTemplate jdbc;
    Long owner, viewer, feed;
    UserPrincipal principal;

    @BeforeEach void prepare() {
        owner = user(); viewer = user();
        principal = new UserPrincipal(viewer, "viewer");
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility,content) VALUES(?,'PUBLIC','public feed')", owner);
        feed = id();
    }

    @Test void eitherBlockDirectionHidesDetailAndStopsLikesButAllowsGuests() {
        for (boolean viewerBlocks : new boolean[]{true, false}) {
            Long blocker = viewerBlocks ? viewer : owner;
            Long blocked = viewerBlocks ? owner : viewer;
            block(blocker, blocked);
            assertThatThrownBy(() -> feeds.getDetail(principal, feed))
                .isInstanceOfSatisfying(BusinessException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(viewerBlocks ? ErrorCode.USER_BLOCKED : ErrorCode.FEED_NOT_FOUND));
            assertThatThrownBy(() -> feeds.like(principal, feed))
                .isInstanceOfSatisfying(BusinessException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(viewerBlocks ? ErrorCode.USER_BLOCKED : ErrorCode.FEED_NOT_FOUND));
            assertThat(feeds.getDetail(null, feed).getFeedId()).isEqualTo(feed);
            blocks.softDelete(blocker, blocked);
        }
    }

    @Test void unblockingRestoresImmediateAccess() {
        block(viewer, owner);
        assertThatThrownBy(() -> feeds.getDetail(principal, feed)).isInstanceOf(BusinessException.class);
        blocks.softDelete(viewer, owner);
        assertThat(feeds.getDetail(principal, feed).getFeedId()).isEqualTo(feed);
    }

    @Test void ownersAndUnrelatedMembersCanStillReadPublicFeeds() {
        block(viewer, owner);
        assertThat(feeds.getDetail(new UserPrincipal(owner, "owner"), feed).getFeedId()).isEqualTo(feed);
        assertThat(feeds.getDetail(new UserPrincipal(user(), "unrelated"), feed).getFeedId()).isEqualTo(feed);
    }

    @Test void guestsStillCannotReadPrivateFeeds() {
        jdbc.update("UPDATE ultary_feed SET visibility='PRIVATE' WHERE feed_id=?", feed);
        assertThatThrownBy(() -> feeds.getDetail(null, feed))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FEED_NOT_FOUND));
    }

    void block(Long blocker, Long blocked) {
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?)", blocker, blocked);
    }
    Long user() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "block_" + UUID.randomUUID().toString().substring(0, 10));
        return id();
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
