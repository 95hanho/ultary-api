package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class FeedThreadLikeIntegrationTest {
    @Autowired FeedService feeds;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.mybatis.spring.SqlSessionTemplate sql;
    Long viewer,feed,comment,reply;
    UserPrincipal principal;

    @BeforeEach void fixture() {
        Long author=user();viewer=user();principal=new UserPrincipal(viewer,"like-test");
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'like test','PUBLIC')",author);feed=id();
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'comment')",feed,author);comment=id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'reply')",comment,author);reply=id();
    }

    @Test void commentLikeSurvivesFreshReadsAndUnlikeAndRepeatedLikeAreIdempotent() {
        feeds.likeComment(principal,feed,comment);feeds.likeComment(principal,feed,comment);
        sql.clearCache();
        var saved=feeds.getComments(principal,feed,20).stream().filter(c->c.getFeedCommentId().equals(comment)).findFirst().orElseThrow();
        assertThat(saved.getLikedByMe()).isTrue();assertThat(saved.getLikeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_feed_comment_like WHERE feed_comment_id=? AND user_no=? AND is_deleted=0",Integer.class,comment,viewer)).isEqualTo(1);
        feeds.unlikeComment(principal,feed,comment);feeds.unlikeComment(principal,feed,comment);sql.clearCache();
        var cleared=feeds.getComments(principal,feed,20).get(0);
        assertThat(cleared.getLikedByMe()).isFalse();assertThat(cleared.getLikeCount()).isZero();
    }

    @Test void replyLikeSurvivesFreshReadsAndCanBeRestoredAfterUnlike() {
        feeds.likeReply(principal,feed,comment,reply);feeds.likeReply(principal,feed,comment,reply);sql.clearCache();
        var saved=feeds.getReplies(principal,feed,comment,20).get(0);
        assertThat(saved.getLikedByMe()).isTrue();assertThat(saved.getLikeCount()).isEqualTo(1);
        feeds.unlikeReply(principal,feed,comment,reply);sql.clearCache();
        assertThat(feeds.getReplies(principal,feed,comment,20).get(0).getLikedByMe()).isFalse();
        feeds.likeReply(principal,feed,comment,reply);sql.clearCache();
        var embedded=feeds.getComments(principal,feed,20).get(0).getReplies().get(0);
        assertThat(embedded.getLikedByMe()).isTrue();assertThat(embedded.getLikeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_feed_reply_like WHERE feed_reply_id=? AND user_no=? AND is_deleted=0",Integer.class,reply,viewer)).isEqualTo(1);
    }

    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    Long user(){jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","like_"+UUID.randomUUID().toString().substring(0,10));return id();}
}
