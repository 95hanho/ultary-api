package me._hanho.ultary.domain.tag;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.tag.model.TagSearchHit;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class TagSuggestIntegrationTest {
    @Autowired TagService tags;
    @Autowired JdbcTemplate jdbc;
    Long owner, viewer;
    String query;

    @BeforeEach void fixture() {
        owner=user(); viewer=user();
        query="가"+UUID.randomUUID().toString().replace("-", "").substring(0,6)+"산책";
    }

    @Test void ranksRatioBeforeVisiblePostCountAndThenAlphabeticallyBeforeApplyingLimit() {
        Long exact=tag(query), course=tag(query+"코스"), weekend=tag("주말"+query);
        Long first=tag("가"+query+"길"), second=tag("나"+query+"길"), longer=tag(query+"매일코스");
        posts(course,3); posts(weekend,2); posts(first,1); posts(second,1); posts(longer,8);
        // Stale use_count must not influence the POST tie breaker.
        jdbc.update("UPDATE ultary_tag SET use_count=9999 WHERE tag_id=?",weekend);
        List<TagSearchHit> result=tags.suggestHashtags(viewer,"#"+query,20);
        assertThat(result).extracting(TagSearchHit::getTagId).containsExactly(exact,course,weekend,first,second,longer);
        assertThat(result).extracting(TagSearchHit::getFeedCount).containsExactly(0,3,2,1,1,8);
        assertThat(tags.suggestHashtags(viewer,query,2)).extracting(TagSearchHit::getTagId).containsExactly(exact,course);
    }

    @Test void countsOnlyVisibleActivePostsAndExcludesUnavailableTags() {
        Long visible=tag(query+"가"), hidden=tag(query+"나");posts(visible,1);posts(hidden,1);
        post(hidden,"PRIVATE",false,false);post(hidden,"PUBLIC",true,false);post(hidden,"PUBLIC",false,true);
        Long pending=tag(query+"대기"), rejected=tag(query+"거절"), deleted=tag(query+"삭제"), suspended=tag(query+"정지");
        jdbc.update("UPDATE ultary_tag SET purpose='COMMERCIAL',profile_status='PENDING' WHERE tag_id=?",pending);
        jdbc.update("UPDATE ultary_tag SET profile_status='REJECTED' WHERE tag_id=?",rejected);
        jdbc.update("UPDATE ultary_tag SET is_deleted=1 WHERE tag_id=?",deleted);
        Long banned=user();jdbc.update("UPDATE ultary_user SET withdrawal_status='SUSPENDED' WHERE user_no=?",banned);
        jdbc.update("UPDATE ultary_tag SET created_by_user_no=? WHERE tag_id=?",banned,suspended);
        var result=tags.suggestHashtags(viewer,query,20);
        assertThat(result).extracting(TagSearchHit::getTagId).containsExactly(visible,hidden);
        assertThat(result).extracting(TagSearchHit::getFeedCount).containsExactly(1,1);
    }

    @Test void underscoreIsLiteralRatherThanASqlWildcard() {
        String term=query+"_";Long literal=tag(term+"코스");tag(query+"가코스");
        assertThat(tags.suggestHashtags(viewer,term,20)).extracting(TagSearchHit::getTagId).containsExactly(literal);
    }

    @Test void blankAndInvalidTokensHaveNoSuggestions() {
        assertThat(tags.suggestHashtags(viewer,null,10)).isEmpty();
        assertThat(tags.suggestHashtags(viewer,"#",10)).isEmpty();
        assertThat(tags.suggestHashtags(viewer,"산%책",10)).isEmpty();
    }

    Long user(){jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","suggest"+UUID.randomUUID().toString().substring(0,12));return id();}
    Long tag(String body){jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,profile_status,created_by_user_no) VALUES(?,'USER','NONE',?)",body,owner);return id();}
    void posts(Long tag,int count){for(int n=0;n<count;n++)post(tag,"PUBLIC",false,false);}
    void post(Long tag,String visibility,boolean deleted,boolean relationDeleted){
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility,is_deleted) VALUES(?,'suggestion test',?,?)",owner,visibility,deleted);
        Long feed=id();jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id,is_deleted) VALUES(?,?,?)",feed,tag,relationDeleted);
    }
    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
