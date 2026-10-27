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
    @Autowired me._hanho.ultary.domain.story.StoryMapper stories;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
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

    @Test void firstUnreadAnchorPrecedesTheLatestPageAndSurvivesMarkingTheRoomRead() {
        var first=service.start(principal,request("already read"));
        Long room=first.room().getDmRoomId();
        Long unread=null;
        for(int i=0;i<40;i++){
            jdbc.update("INSERT INTO ultary_dm_message(dm_room_id,sender_user_no,body) VALUES(?,?,?)",room,peer,"unread "+i);
            if(unread==null)unread=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        }
        sqlSession.clearCache();
        var page=service.messages(principal,room,null,30);
        assertThat(page.getFirstUnreadMessageId()).isEqualTo(unread);
        assertThat(page.getItems()).hasSize(30).noneMatch(m->m.getDmMessageId().equals(page.getFirstUnreadMessageId()));
        var older=service.messages(principal,room,page.getNextCursorMessageId(),30);
        assertThat(older.getItems()).extracting(m->m.getDmMessageId()).contains(unread);
        assertThat(older.getFirstUnreadMessageId()).isNull();
        assertThat(service.messages(principal,room,null,30).getFirstUnreadMessageId()).isNull();
    }

    @Test void unreadAnchorIgnoresMyOwnNewerMessages() {
        Long room=service.start(principal,request("read baseline")).room().getDmRoomId();
        jdbc.update("INSERT INTO ultary_dm_message(dm_room_id,sender_user_no,body) VALUES(?,?,'own message')",room,me);
        sqlSession.clearCache();
        assertThat(service.messages(principal,room,null,30).getFirstUnreadMessageId()).isNull();
    }

    @Test void viewerWithoutReadCursorStartsAtTheFirstIncomingMessage() {
        var first=service.start(principal,request("first incoming message"));
        var peerPrincipal=new UserPrincipal(peer,"unread-peer-test");
        sqlSession.clearCache();
        assertThat(service.messages(peerPrincipal,first.room().getDmRoomId(),null,30).getFirstUnreadMessageId())
            .isEqualTo(first.message().getDmMessageId());
    }

    @Test void leavingHidesOnlyMyRoomAndReopeningPreservesHistory() {
        var first = service.start(principal, request("보존할 메시지"));
        Long room = first.room().getDmRoomId();
        var peerPrincipal = new UserPrincipal(peer, "dm-peer-test");

        service.leave(principal, room);
        assertThat(service.listRooms(principal, 30).getItems()).isEmpty();
        assertThat(service.listRooms(peerPrincipal, 30).getItems())
            .extracting(item -> item.getDmRoomId()).contains(room);
        assertThat(service.messages(peerPrincipal, room, null, 30).getItems())
            .extracting(item -> item.getBody()).containsExactly("보존할 메시지");
        assertThatThrownBy(() -> service.messages(principal, room, null, 30))
            .isInstanceOf(BusinessException.class);

        var reopened = service.start(principal, request("다시 대화"));
        assertThat(reopened.room().getDmRoomId()).isEqualTo(room);
        assertThat(roomCount()).isEqualTo(1);
        assertThat(service.messages(principal, room, null, 30).getItems())
            .extracting(item -> item.getBody()).containsExactly("보존할 메시지", "다시 대화");
    }

    @Test void peerMessageMakesHiddenRoomVisibleAgainWithHistory() {
        var first = service.start(principal, request("첫 메시지"));
        Long room = first.room().getDmRoomId();
        service.leave(principal, room);

        var reply = new StartDmRequest();
        reply.setTargetUserNo(me);
        reply.setBody("상대의 새 메시지");
        var reopened = service.start(new UserPrincipal(peer, "dm-peer-test"), reply);

        assertThat(reopened.room().getDmRoomId()).isEqualTo(room);
        assertThat(service.listRooms(principal, 30).getItems())
            .extracting(item -> item.getDmRoomId()).contains(room);
        assertThat(service.messages(principal, room, null, 30).getItems())
            .extracting(item -> item.getBody()).containsExactly("첫 메시지", "상대의 새 메시지");
    }

    @Test void invalidSharedContentRollsBackRoomCreation() {
        var request = request("공유"); request.setFeedId(-1L);
        assertFailedWithoutRoom(request);
    }

    @Test void changedRelationshipIsCheckedAgainWhenSending() {
        jdbc.update("DELETE FROM ultary_neighbor WHERE requester_user_no=? AND receiver_user_no=?", me, peer);
        assertFailedWithoutRoom(request("보내기"));
    }

    @Test void sharedFeedAndStoryUseTheirActualAuthorAndViewerStoryState() {
        Long author = user();
        String nickname = jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?", String.class, author);
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('dm-share-test.jpg','images/dm-share-test.jpg','image/jpeg','jpg','OWNED',?)", author);
        Long file = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority,profile_file_id) VALUES(?,?,?,1,?)", author, "dmshare"+UUID.randomUUID().toString().substring(0,12), "DM share pet", file);
        jdbc.update("INSERT INTO ultary_user_privacy(user_no,story_visibility) VALUES(?,'PUBLIC')", author);
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility,content) VALUES(?,'PUBLIC','공유 테스트')", author);
        Long feed = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)", feed, file);
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE','2099-12-31 23:59:59')", author, file);
        Long story = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        var feedRequest = request("게시글 공유"); feedRequest.setFeedId(feed);
        var feedSent = service.start(principal, feedRequest);
        var storyRequest = request("스토리 공유"); storyRequest.setStoryId(story);
        var storySent = service.start(principal, storyRequest);
        for (var share : java.util.List.of(feedSent.message().getShare(), storySent.message().getShare())) {
            assertThat(share.getAuthorUserNo()).isEqualTo(author).isNotIn(me, peer);
            assertThat(share.getAuthorNickname()).isEqualTo(nickname);
            assertThat(share.getAuthorProfileFile().getFileId()).isEqualTo(file);
            assertThat(share.isAuthorHasStory()).isTrue();
            assertThat(share.isAuthorHasUnviewed()).isTrue();
        }

        stories.insertViewIgnoreDuplicate(story, me);
        var messages = service.messages(principal, feedSent.room().getDmRoomId(), null, 30).getItems();
        assertThat(messages).hasSize(2);
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getShare().getAuthorUserNo()).isEqualTo(author);
            assertThat(message.getShare().isAuthorHasStory()).isTrue();
            assertThat(message.getShare().isAuthorHasUnviewed()).isFalse();
        });
        stories.softDelete(story, author);
        var withoutStory = service.messages(principal, feedSent.room().getDmRoomId(), null, 30).getItems().get(0).getShare();
        assertThat(withoutStory.isAuthorHasStory()).isFalse();
        assertThat(withoutStory.isAuthorHasUnviewed()).isFalse();
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
