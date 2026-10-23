package me._hanho.ultary.domain.neighbor;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.user.UserBlockMapper;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class NeighborBlockIntegrationTest {
    @Autowired NeighborService neighbors;
    @Autowired NeighborMapper relations;
    @Autowired UserBlockMapper blocks;
    @Autowired JdbcTemplate jdbc;
    Long me, other;
    UserPrincipal principal;

    @BeforeEach void setup() {
        me = user(); other = user(); principal = new UserPrincipal(me, "blocker");
    }

    @Test void blockingRemovesMyFollowAndItsNotification() {
        Long relation = follow(me, other, false);
        jdbc.update("INSERT INTO ultary_notification(receiver_user_no,actor_user_no,type,neighbor_id,group_key) VALUES(?,?,'NEIGHBOR_REQUEST',?,?)",
            other, me, relation, "NEIGHBOR:" + relation);
        assertThat(neighbors.viewerFollows(me, other)).isTrue();
        neighbors.block(principal, other);
        assertThat(blocks.findActiveByPair(me, other)).isNotNull();
        assertThat(relations.findByNeighborId(relation)).isNull();
        assertThat(neighbors.viewerFollows(me, other)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE neighbor_id=?", Integer.class, relation)).isZero();
    }

    @Test void blockingAlsoRemovesMutualFollowsWhenIWasTheReceiver() {
        follow(other, me, true);
        assertThat(neighbors.viewerFollows(me, other)).isTrue();
        assertThat(neighbors.viewerFollows(other, me)).isTrue();
        neighbors.block(principal, other);
        assertThat(neighbors.viewerFollows(me, other)).isFalse();
        assertThat(neighbors.viewerFollows(other, me)).isFalse();
    }

    @Test void unblockingDoesNotRestoreTheRemovedFollowAndAllowsBlockingAgain() {
        follow(me, other, false);
        neighbors.block(principal, other);
        neighbors.unblock(principal, other);
        assertThat(blocks.findActiveByPair(me, other)).isNull();
        assertThat(neighbors.viewerFollows(me, other)).isFalse();
        neighbors.block(principal, other);
        assertThat(blocks.findActiveByPair(me, other)).isNotNull();
    }

    Long follow(Long requester, Long receiver, boolean mutual) {
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at,followed_back) VALUES(?,?,?,'ACCEPTED',NOW(),?)",
            requester, receiver, NeighborService.pairKey(requester, receiver), mutual ? 1 : 0);
        return id();
    }
    Long user() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "block_" + UUID.randomUUID().toString().substring(0, 10));
        return id();
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
