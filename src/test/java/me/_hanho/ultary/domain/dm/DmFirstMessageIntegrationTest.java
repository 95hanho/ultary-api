package me._hanho.ultary.domain.dm;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.dm.dto.request.StartDmRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class DmFirstMessageIntegrationTest {
    @Autowired DmService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    Long me, peer;
    UserPrincipal principal;

    @BeforeEach void setup() {
        me = user(); peer = user(); principal = new UserPrincipal(me, "dm-first-test");
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at) VALUES(?,?,?,'ACCEPTED',NOW())", me,peer,me+":"+peer);
    }

    @Test void firstMessageCreatesRoomAndLaterMessagesReuseIt() {
        assertThat(roomCount()).isZero();
        var first = service.start(principal, request("첫 메시지"));
        Long room = first.room().getDmRoomId();
        assertThat(roomCount()).isEqualTo(1);
        assertThat(first.message().getBody()).isEqualTo("첫 메시지");
        assertThat(first.room().getLastMessage()).isEqualTo("첫 메시지");
        var second = service.start(principal, request("다음 메시지"));
        assertThat(second.room().getDmRoomId()).isEqualTo(room);
        assertThat(roomCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_dm_message WHERE dm_room_id=?", Integer.class, room)).isEqualTo(2);
    }

    @Test void emptyMessageRollsBackRoomCreation() {
        assertFailedWithoutRoom(request("   "));
    }

    @Test void invalidSharedContentRollsBackRoomCreation() {
        var request = request("공유"); request.setFeedId(-1L);
        assertFailedWithoutRoom(request);
    }

    @Test void changedRelationshipIsCheckedAgainWhenSending() {
        jdbc.update("DELETE FROM ultary_neighbor WHERE requester_user_no=? AND receiver_user_no=?", me, peer);
        assertFailedWithoutRoom(request("보내기"));
    }

    void assertFailedWithoutRoom(StartDmRequest request) {
        // Savepoint isolates the request's rollback while all fixture data still rolls back after the test.
        var nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(() -> nested.execute(status -> service.start(principal, request)))
            .isInstanceOf(BusinessException.class);
        assertThat(roomCount()).isZero();
    }
    int roomCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM ultary_dm_room WHERE user_low=? AND user_high=?", Integer.class, me, peer); }
    StartDmRequest request(String body) { var request = new StartDmRequest(); request.setTargetUserNo(peer); request.setBody(body); return request; }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "dmfirst"+UUID.randomUUID().toString().substring(0,12)); return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
