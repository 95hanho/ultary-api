package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class FeedEditIntegrationTest {
    @Autowired FeedService feeds;
    @Autowired FeedMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.validation.Validator validator;
    @Autowired me._hanho.ultary.domain.ai.ModerationMapper moderation;
    @Autowired me._hanho.ultary.domain.ai.ModerationTransactions transactions;
    @Autowired me._hanho.ultary.domain.settings.PrivacyService privacy;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
    Long owner, other, feed, file, media, secondMedia, foreignMedia, oldPet, newPet, collaborator;
    UserPrincipal principal;

    @BeforeEach void fixture() {
        owner=user(); other=user(); principal=new UserPrincipal(owner,"test");
        oldPet=pet(owner); newPet=pet(other); collaborator=pet(owner);
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'original','PUBLIC')",owner); feed=id();
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('edit.jpg','images/edit.jpg','image/jpeg','jpg','OWNED',?)",owner); file=id();
        media=media(feed,0); secondMedia=media(feed,1);
        jdbc.update("INSERT INTO ultary_feed(user_no,content) VALUES(?,'other')",other); foreignMedia=media(id(),0);
        jdbc.update("INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no) VALUES(?,?,10,20,?)",media,oldPet,owner);
        jdbc.update("INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no) VALUES(?,?,30,40,?)",secondMedia,collaborator,owner);
        jdbc.update("INSERT INTO ultary_feed_pet(feed_id,pet_id,added_by_user_no,role,is_main) VALUES(?,?,?,'TAGGED',1)",feed,oldPet,owner);
        jdbc.update("INSERT INTO ultary_feed_pet(feed_id,pet_id,added_by_user_no,role) VALUES(?,?,?,'COLLABORATOR')",feed,collaborator,owner);
    }

    @Test void editsCaptionAndPhotoTagsWithoutChangingFilesOrOrderAndKeepsCollaborators() {
        var request=new UpdateFeedRequest(); request.setContent("changed");
        request.setMediaTags(List.of(tags(media,newPet), tags(secondMedia,null)));
        var result=feeds.update(principal,feed,request);
        assertThat(result.getContent()).isEqualTo("changed");
        assertThat(result.getMedia()).extracting(x->x.getFeedMediaId()).containsExactly(media,secondMedia);
        assertThat(result.getMedia()).allSatisfy(x->assertThat(x.getFileId()).isEqualTo(file));
        assertThat(result.getMedia().get(0).getMentions().get(0).getPetId()).isEqualTo(newPet);
        assertThat(result.getMedia().get(0).getMentions().get(0).getMentionId()).isNotBlank();
        assertThat(result.getMedia().get(1).getMentions()).isEmpty();
        assertThat(mapper.findPetsByFeedId(feed)).extracting(x->x.getPetId()).containsExactlyInAnyOrder(newPet,collaborator);
        assertThat(mapper.findPetsByFeedId(feed).stream().filter(x->x.getPetId().equals(collaborator)).findFirst().orElseThrow().getRole()).isEqualTo("COLLABORATOR");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND group_key=?",Integer.class,other,"FEED_TAG:"+feed)).isEqualTo(1);

        var clear=new UpdateFeedRequest();clear.setMediaTags(List.of(tags(media,null),tags(secondMedia,null)));
        feeds.update(principal,feed,clear);
        assertThat(mapper.findMentionsByFeedMediaId(media)).isEmpty();
        assertThat(mapper.findPetsByFeedId(feed)).extracting(x->x.getPetId()).containsExactly(collaborator);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND group_key=?",Integer.class,other,"FEED_TAG:"+feed)).isZero();
    }

    @Test void rejectsAnotherAuthorsEditAndAnotherFeedsMedia() {
        var request=new UpdateFeedRequest();request.setMediaTags(List.of(tags(media,newPet)));
        assertThatThrownBy(()->feeds.update(new UserPrincipal(other,"other"),feed,request))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        request.setMediaTags(List.of(tags(foreignMedia,newPet)));
        assertThatThrownBy(()->feeds.update(principal,feed,request))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(mapper.findMentionsByFeedMediaId(foreignMedia)).isEmpty();
    }

    @Test void preservesTagsWhenOmittedAndValidatesPhotoCoordinates() {
        var request=new UpdateFeedRequest(); request.setContent("caption only");
        feeds.update(principal,feed,request);
        assertThat(mapper.findMentionsByFeedMediaId(media)).extracting(x->x.getPetId()).containsExactly(oldPet);
        var invalid=tags(media,newPet); invalid.getMentions().get(0).setPosX(new BigDecimal("100.01"));
        request.setMediaTags(List.of(invalid));
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test void replacesHashtagLinksAndCanRestoreARemovedTagWithoutDoubleCounting() {
        Long a=tag(),b=tag();
        var request=new UpdateFeedRequest(); request.setTagIds(List.of(a)); feeds.update(principal,feed,request);
        request.setTagIds(List.of(b));feeds.update(principal,feed,request);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(b);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,a)).isZero();
        request.setTagIds(List.of(a)); feeds.update(principal,feed,request); feeds.update(principal,feed,request);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(a);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,a)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,b)).isZero();
    }

    @Test void rejectedCaptionRestoresItsHashtagLinksAlongWithTheApprovedText() {
        Long a=tag(),b=tag();
        String oldText="#"+jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?",String.class,a);
        String newText="#"+jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?",String.class,b);
        jdbc.update("UPDATE ultary_feed SET content=? WHERE feed_id=?",oldText,feed);
        var initial=new UpdateFeedRequest();initial.setTagIds(List.of(a));feeds.update(principal,feed,initial);
        var edit=new UpdateFeedRequest();edit.setContent(newText);edit.setTagIds(List.of(b));feeds.update(principal,feed,edit);
        Long job=jdbc.queryForObject("SELECT job_id FROM ultary_ai_moderation_job WHERE target_type='FEED' AND target_id=?",Long.class,feed);
        transactions.complete(transactions.claim(job),new me._hanho.ultary.domain.ai.ModerationResult(false,"ABUSE",null,null,false));
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed WHERE feed_id=?",String.class,feed)).isEqualTo(oldText);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(a);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,b)).isZero();
    }

    @Test void createsWithAccountDefaultOrExplicitPostVisibility() {
        var defaults = new me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest();
        defaults.setFeedVisibility("PRIVATE");
        privacy.update(principal, defaults);
        var request = new CreateFeedRequest();
        var image = new CreateFeedRequest.MediaItem();
        image.setFileId(file); image.setMediaType("IMAGE");
        request.setMedia(List.of(image));
        assertThat(feeds.create(principal, request).getVisibility()).isEqualTo("PRIVATE");
        for (String visibility : List.of("PUBLIC", "NEIGHBORS", "PRIVATE")) {
            request.setVisibility(visibility);
            var created = feeds.create(principal, request);
            assertThat(created.getVisibility()).isEqualTo(visibility);
            assertThat(mapper.findActiveByFeedId(created.getFeedId()).getVisibility()).isEqualTo(visibility);
        }
        assertThat(privacy.get(principal).getFeedVisibility()).isEqualTo("PRIVATE");
    }

    @Test void updatesVisibilityAndEnforcesAudienceWithoutChangingCaptionOrPhotos() {
        var viewer = new UserPrincipal(other, "other");
        assertThat(feeds.getDetail(null, feed).getVisibility()).isEqualTo("PUBLIC");
        var request = new UpdateFeedRequest(); request.setVisibility("NEIGHBORS");
        assertThat(feeds.update(principal, feed, request).getVisibility()).isEqualTo("NEIGHBORS");
        assertThatThrownBy(() -> feeds.getDetail(viewer, feed)).isInstanceOf(BusinessException.class);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,requested_at,accepted_at) VALUES(?,?,?,'ACCEPTED',NOW(),NOW())",
                other, owner, Math.min(owner, other) + ":" + Math.max(owner, other));
        // JDBC fixture changes bypass MyBatis's transaction-local query cache.
        sqlSession.clearCache();
        assertThat(feeds.getDetail(viewer, feed).getVisibility()).isEqualTo("NEIGHBORS");
        request.setVisibility("PRIVATE");
        feeds.update(principal, feed, request);
        assertThatThrownBy(() -> feeds.getDetail(viewer, feed)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> feeds.getDetail(null, feed)).isInstanceOf(BusinessException.class);
        var own = feeds.getDetail(principal, feed);
        assertThat(own.getVisibility()).isEqualTo("PRIVATE");
        assertThat(own.getContent()).isEqualTo("original");
        assertThat(own.getMedia()).extracting(x -> x.getFeedMediaId()).containsExactly(media, secondMedia);
        request.setVisibility("PUBLIC");
        feeds.update(principal, feed, request);
        assertThat(feeds.getDetail(null, feed).getVisibility()).isEqualTo("PUBLIC");
    }

    UpdateFeedRequest.MediaTags tags(Long slot,Long pet) {
        var tags=new UpdateFeedRequest.MediaTags();tags.setFeedMediaId(slot);
        if(pet==null)tags.setMentions(List.of());
        else {var mention=new CreateFeedRequest.MentionItem();mention.setPetId(pet);mention.setPosX(new BigDecimal("25.50"));mention.setPosY(new BigDecimal("70.00"));tags.setMentions(List.of(mention));}
        return tags;
    }
    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    Long user(){jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","edit"+UUID.randomUUID().toString().substring(0,12));return id();}
    Long pet(Long user){jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,1)",user,"edit"+UUID.randomUUID().toString().replace("-","").substring(0,12),"edit pet");return id();}
    Long media(Long parent,int order){jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',?)",parent,file,order);return id();}
    Long tag(){jdbc.update("INSERT INTO ultary_tag(hashtag,created_by_user_no) VALUES(?,?)","edit"+UUID.randomUUID().toString().replace("-","").substring(0,12),owner);return id();}
}
