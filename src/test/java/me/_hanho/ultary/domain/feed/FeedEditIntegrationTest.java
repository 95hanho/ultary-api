package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
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
    @Autowired FeedCaptionTags captionTags;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.validation.Validator validator;
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

    @Test void reordersMediaWhenFullPermutationIsSentAndRejectsIncompleteSets() {
        var omit = new UpdateFeedRequest(); omit.setContent("order untouched");
        assertThat(feeds.update(principal, feed, omit).getMedia())
                .extracting(x -> x.getFeedMediaId()).containsExactly(media, secondMedia);

        var reorder = new UpdateFeedRequest();
        reorder.setMediaOrder(List.of(secondMedia, media));
        var result = feeds.update(principal, feed, reorder);
        assertThat(result.getMedia()).extracting(x -> x.getFeedMediaId()).containsExactly(secondMedia, media);
        assertThat(result.getMedia()).extracting(x -> x.getSortOrder()).containsExactly(0, 1);
        assertThat(result.getMedia()).allSatisfy(x -> assertThat(x.getFileId()).isEqualTo(file));
        assertThat(jdbc.queryForObject("SELECT sort_order FROM ultary_feed_media WHERE feed_media_id=?", Integer.class, secondMedia)).isZero();
        assertThat(jdbc.queryForObject("SELECT sort_order FROM ultary_feed_media WHERE feed_media_id=?", Integer.class, media)).isEqualTo(1);

        var missing = new UpdateFeedRequest(); missing.setMediaOrder(List.of(secondMedia));
        assertThatThrownBy(() -> feeds.update(principal, feed, missing))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        var duplicate = new UpdateFeedRequest(); duplicate.setMediaOrder(List.of(secondMedia, secondMedia));
        assertThatThrownBy(() -> feeds.update(principal, feed, duplicate))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        var foreign = new UpdateFeedRequest(); foreign.setMediaOrder(List.of(secondMedia, foreignMedia));
        assertThatThrownBy(() -> feeds.update(principal, feed, foreign))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(mapper.findMediaByFeedId(feed)).extracting(x -> x.getFeedMediaId()).containsExactly(secondMedia, media);
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

    @Test void rejectedCaptionPreservesItsHashtagLinksUntilSynchronousApproval() {
        Long a=tag(),b=tag();
        String oldText="#"+jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?",String.class,a);
        String newText="#"+jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?",String.class,b);
        jdbc.update("UPDATE ultary_feed SET content=? WHERE feed_id=?",oldText,feed);
        var initial=new UpdateFeedRequest();initial.setTagIds(List.of(a));feeds.update(principal,feed,initial);
        var ai=mock(me._hanho.ultary.domain.ai.AiService.class);
        // Keep the test's rollback transaction; only the external AI call is mocked.
        var inspectedFeeds=new FeedCreationService(ai,feeds,captionTags);
        var edit=new UpdateFeedRequest();edit.setContent(newText);edit.setTagIds(List.of(b));
        doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED)).when(ai).assertFeedUpdateAllowed(owner,feed,newText);
        assertThatThrownBy(()->inspectedFeeds.update(principal,feed,edit))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_CONTENT_BLOCKED));
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed WHERE feed_id=?",String.class,feed)).isEqualTo(oldText);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(a);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,a)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,b)).isZero();
        doNothing().when(ai).assertFeedUpdateAllowed(owner,feed,newText);
        inspectedFeeds.update(principal,feed,edit);
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed WHERE feed_id=?",String.class,feed)).isEqualTo(newText);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(b);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,a)).isZero();
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,b)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_ai_moderation_job WHERE target_type='FEED' AND target_id=?",Integer.class,feed)).isZero();
    }

    @Test void singleCharacterHashtagsRemainTextWithoutCreatingOrLinkingTags() {
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,created_by_user_no) VALUES('힣','USER',?)", owner);
        Long legacy = id(), valid = tag();
        String body = jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?", String.class, valid);
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag", Integer.class);
        String content = "본문 #힣 #a #1 #_ #" + body;
        var request = new CreateFeedRequest();
        var image = new CreateFeedRequest.MediaItem();
        image.setFileId(file); image.setMediaType("IMAGE");
        request.setMedia(List.of(image)); request.setContent(content); request.setTagIds(List.of(legacy));
        assertThat(captionTags.inspectionText(content, List.of(legacy))).isEqualTo(content);
        var created = feeds.create(principal, request);
        assertThat(created.getContent()).isEqualTo(content);
        assertThat(mapper.findTagIdsByFeedId(created.getFeedId())).containsExactly(valid);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag", Integer.class)).isEqualTo(before);

        var update = new UpdateFeedRequest(); update.setContent("수정 #힣 #a #1 #_");
        update.setTagIds(List.of(legacy));
        var updated = feeds.update(principal, created.getFeedId(), update);
        assertThat(updated.getContent()).isEqualTo(update.getContent());
        assertThat(mapper.findTagIdsByFeedId(created.getFeedId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?", Integer.class, valid)).isZero();
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?", Integer.class, legacy)).isZero();
        feeds.restoreCaptionTags(created.getFeedId(), content);
        assertThat(mapper.findTagIdsByFeedId(created.getFeedId())).containsExactly(valid);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag", Integer.class)).isEqualTo(before);
        assertThat("힣".matches(me._hanho.ultary.common.validation.HashtagRules.REGEX)).isFalse();
        assertThat("산책".matches(me._hanho.ultary.common.validation.HashtagRules.REGEX)).isTrue();
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

    @Test void reservedAccountCanEditAnotherFeedWithoutTakingOwnershipOrChangingSubjectOwner() {
        var moderator = new UserPrincipal(100L, "administrator");
        var request = new UpdateFeedRequest();
        request.setContent("관리자가 수정한 게시글");
        request.setSubjectPetIds(List.of(oldPet));
        var ai = mock(me._hanho.ultary.domain.ai.AiService.class);
        var result = new FeedCreationService(ai, feeds, captionTags).update(moderator, feed, request);
        assertThat(result.getContent()).isEqualTo(request.getContent());
        assertThat(result.getUserNo()).isEqualTo(owner);
        assertThat(result.getSubjectPets()).extracting(x -> x.petId()).containsExactly(oldPet);
        verify(ai).assertFeedUpdateAllowed(100L, feed, request.getContent());
        assertThatThrownBy(() -> feeds.delete(moderator, feed)).isInstanceOf(BusinessException.class);
    }

    @Test void reservedAccountEditsCommentsAndRepliesButOtherUsersCannot() {
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'before')", feed, owner);
        Long comment = id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'before')", comment, owner);
        Long reply = id();
        var commentRequest = new me._hanho.ultary.domain.feed.dto.request.UpdateCommentRequest();
        commentRequest.setContent("수정된 댓글");
        var replyRequest = new me._hanho.ultary.domain.feed.dto.request.UpdateReplyRequest();
        replyRequest.setContent("수정된 답글");
        var stranger = new UserPrincipal(other, "administrator");
        assertThatThrownBy(() -> feeds.updateComment(stranger, feed, comment, commentRequest))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> feeds.updateReply(stranger, feed, comment, reply, replyRequest))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        var moderator = new UserPrincipal(100L, "administrator");
        feeds.updateComment(moderator, feed, comment, commentRequest);
        feeds.updateReply(moderator, feed, comment, reply, replyRequest);
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed_comment WHERE feed_comment_id=?", String.class, comment)).isEqualTo(commentRequest.getContent());
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed_reply WHERE feed_reply_id=?", String.class, reply)).isEqualTo(replyRequest.getContent());
        assertThat(jdbc.queryForObject("SELECT user_no FROM ultary_feed_comment WHERE feed_comment_id=?", Long.class, comment)).isEqualTo(owner);
        assertThat(jdbc.queryForObject("SELECT user_no FROM ultary_feed_reply WHERE feed_reply_id=?", Long.class, reply)).isEqualTo(owner);
        assertThatThrownBy(() -> feeds.deleteComment(moderator, feed, comment)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> feeds.deleteReply(moderator, feed, comment, reply)).isInstanceOf(BusinessException.class);
    }

    @Test void rejectedCaptionCreatesNeitherNewTagsNorFeedChanges() {
        String hashtag="new"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        var ai=mock(me._hanho.ultary.domain.ai.AiService.class);
        var inspected=new FeedCreationService(ai,feeds,captionTags);
        var edit=new UpdateFeedRequest();edit.setContent("본문 #"+hashtag);
        doThrow(new BusinessException(ErrorCode.AI_CONTENT_BLOCKED)).when(ai)
            .assertFeedUpdateAllowed(owner,feed,edit.getContent());
        assertThatThrownBy(()->inspected.update(principal,feed,edit)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag WHERE hashtag=?",Integer.class,hashtag)).isZero();
        assertThat(jdbc.queryForObject("SELECT content FROM ultary_feed WHERE feed_id=?",String.class,feed)).isEqualTo("original");
        doNothing().when(ai).assertFeedUpdateAllowed(owner,feed,edit.getContent());
        inspected.update(principal,feed,edit);
        assertThat(mapper.findTagIdsByFeedId(feed)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag WHERE hashtag=?",Integer.class,hashtag)).isEqualTo(1);
        inspected.update(principal,feed,edit);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE hashtag=?",Integer.class,hashtag)).isEqualTo(1);
        edit.setContent("태그를 지웠어요");inspected.update(principal,feed,edit);
        assertThat(mapper.findTagIdsByFeedId(feed)).isEmpty();
    }

    @Test void approvedCreateDerivesAndDeduplicatesTagsFromTheWholeCaption() {
        String hashtag="new"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        var ai=mock(me._hanho.ultary.domain.ai.AiService.class);
        var request=new CreateFeedRequest();request.setContent("본문 #"+hashtag+" #"+hashtag.toUpperCase());
        var item=new CreateFeedRequest.MediaItem();item.setFileId(file);request.setMedia(List.of(item));
        var result=new FeedCreationService(ai,feeds,captionTags).create(principal,request);
        verify(ai).assertFeedCreationAllowed(owner,request.getContent());
        assertThat(result.getTagIds()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,result.getTagIds().get(0))).isEqualTo(1);
    }

    @Test void captionOnlyEditRetainsAnAlreadyLinkedPendingTagWithoutRecreatingIt() {
        Long existing=tag();
        String body=jdbc.queryForObject("SELECT hashtag FROM ultary_tag WHERE tag_id=?",String.class,existing);
        jdbc.update("UPDATE ultary_tag SET purpose='COMMERCIAL',profile_status='PENDING',use_count=1 WHERE tag_id=?",existing);
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)",feed,existing);
        var edit=new UpdateFeedRequest();edit.setContent("본문 수정 #"+body);
        feeds.update(principal,feed,edit);
        assertThat(mapper.findTagIdsByFeedId(feed)).containsExactly(existing);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag WHERE hashtag=?",Integer.class,body)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT use_count FROM ultary_tag WHERE tag_id=?",Integer.class,existing)).isEqualTo(1);
    }

    @Test void failedFeedWriteRollsBackItsNewTagsToo() {
        String hashtag="new"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM ultary_feed",Integer.class);
        var request=new CreateFeedRequest();request.setContent("본문 #"+hashtag);
        request.setTagIds(List.of(Long.MAX_VALUE));
        var item=new CreateFeedRequest.MediaItem();item.setFileId(file);request.setMedia(List.of(item));
        var nested=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(()->nested.execute(status->feeds.create(principal,request))).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_tag WHERE hashtag=?",Integer.class,hashtag)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_feed",Integer.class)).isEqualTo(count);
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
