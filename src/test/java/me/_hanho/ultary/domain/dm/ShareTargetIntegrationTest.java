package me._hanho.ultary.domain.dm;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.dm.dto.response.DmShareTargetItemResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "ai.moderation.worker-enabled=false")
@Transactional
class ShareTargetIntegrationTest {
    @Autowired DmService service;
    @Autowired JdbcTemplate jdbc;
    Long me, file;
    LocalDateTime now;
    UserPrincipal principal;

    @BeforeEach void setup() {
        me = user(); now = LocalDateTime.now().withNano(0);
        principal = new UserPrincipal(me, "share-test");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('test.jpg','images/test.jpg','image/jpeg','jpg','OWNED',?)", me);
        file = id();
    }

    @Test void ranksAllNineGroupsAndDeduplicatesMultiplePetsAndInteractions() {
        List<Long> peers = new ArrayList<>();
        for (int i = 0; i < 9; i++) peers.add(user());
        dm(peers.get(0), now.minusDays(20));
        story(me, peers.get(0), now); // DM still wins
        story(me, peers.get(1), now.minusDays(18));
        story(me, peers.get(1), now.minusDays(17)); // same owner, another pet
        feed(me, peers.get(2), now.minusDays(15));
        photoTag(me, peers.get(2), now.minusDays(14));
        story(peers.get(3), me, now.minusDays(12));
        photoTag(peers.get(4), me, now.minusDays(10));
        follow(me, peers.get(5), now.minusDays(8));
        follow(peers.get(6), me, now.minusDays(6));
        follow(me, peers.get(7), now.minusMonths(2));
        follow(peers.get(8), me, now.minusMonths(2));
        var result = list(null, null, 50);
        assertThat(result).extracting(DmShareTargetItemResponse::getUserNo).containsExactlyElementsOf(peers);
        assertThat(result).extracting(DmShareTargetItemResponse::getGroup).containsExactly(1,2,3,4,5,6,7,8,9);
        assertThat(result.get(1).getPetTags()).hasSize(2);
        assertThat(result.get(2).getActiveAt()).isEqualTo(now.minusDays(14));
    }

    @Test void swapsOnlyFirstThreeGroupsAndUsesNewestTimeWithinGroup() {
        Long dm = user(), story = user(), feed = user(), newerStory = user();
        dm(dm, now); story(me, story, now.minusDays(2));
        story(me, newerStory, now.minusDays(1)); feed(me, feed, now);
        assertThat(list(null, "OWN_STORY_TAG,OWN_FEED_TAG,DM", 50))
            .extracting(DmShareTargetItemResponse::getUserNo).containsExactly(newerStory, story, feed, dm);
        assertThatThrownBy(() -> list(null, "DM,DM,OWN_FEED_TAG", 50)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> list(null, "DM,OWN_STORY_TAG", 50)).isInstanceOf(BusinessException.class);
    }

    @Test void searchesBeforeLimitByNicknameAndPetTagCaseInsensitively() {
        for (int i = 0; i < 3; i++) follow(me, user(), now);
        Long peer = user(); follow(me, peer, now.minusMonths(2));
        jdbc.update("UPDATE ultary_user SET nickname='검색대상' WHERE user_no=?", peer);
        Long pet = pet(peer);
        jdbc.update("UPDATE ultary_pet SET mention_id='Share_Search_Pet' WHERE pet_id=?", pet);
        assertThat(list("  @share_SEARCH  ", null, 1)).extracting(DmShareTargetItemResponse::getUserNo).containsExactly(peer);
        assertThat(list("검색대상", null, 1)).hasSize(1);
        assertThat(list("%not-a-wildcard", null, 50)).isEmpty();
        jdbc.update("UPDATE ultary_pet SET is_deleted=1 WHERE pet_id=?", pet);
        assertThat(list("share_search", null, 50)).isEmpty();
    }

    @Test void excludesOldDmOnlyOldTagsDeletedContentSelfWithdrawnAndBothBlockDirections() {
        Long oldDm = user(); dm(oldDm, now.minusMonths(2));
        story(me, user(), now.minusMonths(2));
        Long deleted = user(); Long storyId = story(me, deleted, now);
        jdbc.update("UPDATE ultary_story SET is_deleted=1 WHERE story_id=?", storyId);
        Long deletedFeed = user(); Long feedId = feed(me, deletedFeed, now);
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?", feedId);
        story(me, me, now);
        Long withdrawn = user(); follow(me, withdrawn, now);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='WITHDRAWN' WHERE user_no=?", withdrawn);
        Long blocked = user(), blocker = user(); follow(me, blocked, now); follow(blocker, me, now);
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?),(?,?)",me,blocked,blocker,me);
        assertThat(list(null, null, 50)).isEmpty();
    }

    @Test void includesExpiredStoryHistoryAndKeepsFollowBeforeFollowerForOldMutuals() {
        Long peer = user(); Long sid = story(me, peer, now.minusDays(10));
        jdbc.update("UPDATE ultary_story SET expires_at=? WHERE story_id=?", now.minusDays(9), sid);
        Long mutual = user(); follow(me, mutual, now.minusMonths(3));
        jdbc.update("UPDATE ultary_neighbor SET followed_back=1,followed_back_at=? WHERE requester_user_no=? AND receiver_user_no=?", now.minusMonths(2), me, mutual);
        var result = list(null, null, 50);
        assertThat(result).extracting(DmShareTargetItemResponse::getGroup).containsExactly(2,8);
        assertThat(result.get(1).isFollowing()).isTrue();
        assertThat(result.get(1).isFollowedBy()).isTrue();
    }

    List<DmShareTargetItemResponse> list(String query, String order, int limit) {
        return service.shareTargets(principal, limit, query, order).getItems();
    }

    @Test void eachPickerUsesItsOwnFirstPriorityAndTagPickersKeepSelectableOwnPets() {
        Long dmPeer = user(), storyPeer = user(), feedPeer = user();
        pet(dmPeer); Long ownPet = pet(me);
        dm(dmPeer, now); story(me, storyPeer, now); feed(me, feedPeer, now);
        assertThat(service.shareTargets(principal, 50, null, null, "DM").getItems())
            .extracting(DmShareTargetItemResponse::getUserNo).containsExactly(dmPeer, storyPeer, feedPeer);
        var storyTargets = service.shareTargets(principal, 50, null, null, "STORY_TAG").getItems();
        assertThat(storyTargets).extracting(DmShareTargetItemResponse::getUserNo)
            .containsExactly(storyPeer, dmPeer, feedPeer, me);
        assertThat(storyTargets.get(3).getPets()).extracting(DmShareTargetItemResponse.PetChoice::petId).containsExactly(ownPet);
        assertThat(service.shareTargets(principal, 50, null, null, "FEED_TAG").getItems())
            .extracting(DmShareTargetItemResponse::getUserNo).containsExactly(feedPeer, dmPeer, storyPeer, me);
        assertThatThrownBy(() -> service.shareTargets(principal, 50, null, null, "OTHER"))
            .isInstanceOf(BusinessException.class);
    }

    @Test void tagPickersExcludeAccountsWithoutPetsAndDisallowedTagsButAllowOwnTags() {
        Long noPets = user(), closed = user(), active = user();
        follow(me, noPets, now); follow(me, closed, now); follow(me, active, now);
        pet(closed); pet(active); pet(me);
        jdbc.update("INSERT INTO ultary_user_privacy(user_no,allow_tag) VALUES(?,0),(?,0)", closed, me);
        var result = service.shareTargets(principal, 50, null, null, "FEED_TAG").getItems();
        assertThat(result).extracting(DmShareTargetItemResponse::getUserNo).containsExactly(active, me);
        assertThat(service.shareTargets(principal, 50, null, null, "DM").getItems())
            .extracting(DmShareTargetItemResponse::getUserNo).contains(noPets, closed, active).doesNotContain(me);
        String ownTag = result.get(1).getPets().get(0).mentionId();
        assertThat(service.shareTargets(principal, 1, "@" + ownTag.toUpperCase(), null, "STORY_TAG").getItems())
            .extracting(DmShareTargetItemResponse::getUserNo).containsExactly(me);
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "share" + UUID.randomUUID().toString().substring(0,12)); return id(); }
    Long pet(Long owner) { jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,1)", owner,"share"+UUID.randomUUID().toString().replace("-", "").substring(0,12),"share pet"); return id(); }
    void follow(Long from, Long to, LocalDateTime at) {
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at) VALUES(?,?,?,'ACCEPTED',?)",from,to,Math.min(from,to)+":"+Math.max(from,to),at);
    }
    void dm(Long peer, LocalDateTime at) {
        jdbc.update("INSERT INTO ultary_dm_room(pair_key,user_low,user_high) VALUES(?,?,?)",Math.min(me,peer)+":"+Math.max(me,peer),Math.min(me,peer),Math.max(me,peer));
        Long room = id();
        jdbc.update("INSERT INTO ultary_dm_message(dm_room_id,sender_user_no,body,created_at) VALUES(?,?,'test',?)",room,peer,at);
    }
    Long story(Long author, Long target, LocalDateTime at) {
        Long pet = pet(target);
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,created_at,expires_at) VALUES(?,?,'IMAGE',?,?)",author,file,at,at.plusDays(1)); Long story = id();
        jdbc.update("INSERT INTO ultary_story_mention(story_id,pet_id,pos_x,pos_y,added_by_user_no,created_at) VALUES(?,?,10,20,?,?)",story,pet,author,at);
        return story;
    }
    Long feed(Long author, Long target, LocalDateTime at) {
        Long pet = pet(target);
        jdbc.update("INSERT INTO ultary_feed(user_no,created_at) VALUES(?,?)",author,at); Long feed = id();
        jdbc.update("INSERT INTO ultary_feed_pet(feed_id,pet_id,added_by_user_no,role,created_at) VALUES(?,?,?,'TAGGED',?)",feed,pet,author,at);
        return feed;
    }
    void photoTag(Long author, Long target, LocalDateTime at) {
        Long pet = pet(target);
        jdbc.update("INSERT INTO ultary_feed(user_no,created_at) VALUES(?,?)",author,at); Long feed = id();
        jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)",feed,file); Long media = id();
        jdbc.update("INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no,created_at) VALUES(?,?,10,20,?,?)",media,pet,author,at);
    }
}
