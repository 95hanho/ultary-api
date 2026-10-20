package me._hanho.ultary.domain.feed;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.ai.*;
import me._hanho.ultary.domain.ai.model.ModerationJob;
import me._hanho.ultary.domain.feed.dto.request.UpdateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateReplyRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

/** Real MariaDB/MyBatis transactions; every content fixture rolls back. No Gemini calls. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class FeedServiceModerationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired FeedService feeds;
    @Autowired ModerationQueue queue;
    @Autowired ModerationMapper mapper;
    @Autowired ModerationTransactions transactions;
    Long user,feed,comment;
    @BeforeEach void fixture() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","aitest"+UUID.randomUUID().toString().substring(0,12));
        user=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ultary_feed(user_no,content,comment_count) VALUES(?,?,1)",user,"fixture");
        feed=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,?)",feed,user,"original");
        comment=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    }
    Long job(String kind,Long id,int version) {
        return jdbc.queryForObject("SELECT job_id FROM ultary_ai_moderation_job WHERE target_type=? AND target_id=? AND content_version=?",Long.class,kind,id,version);
    }
    String content() {return jdbc.queryForObject("SELECT content FROM ultary_feed_comment WHERE feed_comment_id=?",String.class,comment);}
    void write(String text,String previous,boolean created) {
        jdbc.update("UPDATE ultary_feed_comment SET content=? WHERE feed_comment_id=?",text,comment);
        queue.enqueue("COMMENT",comment,feed,user,previous,text,created);
    }
    ModerationJob claim(int version) {return transactions.claim(job("COMMENT",comment,version));}
    ModerationResult unsafe() {return new ModerationResult(false,"ABUSE",null,null,false);}

    @Test void saveReturnsWithADurablePendingJobBeforeAnyAiResult() {
        var request=new UpdateCommentRequest();request.setContent("changed");
        var response=feeds.updateComment(new UserPrincipal(user,"test"),feed,comment,request);
        assertThat(response.getContent()).isEqualTo("changed");
        assertThat(content()).isEqualTo("changed");
        assertThat(mapper.findJob(job("COMMENT",comment,1)).getStatus()).isEqualTo("PENDING");
        assertThat(mapper.lockState("COMMENT",comment).getApprovedContent()).isEqualTo("original");
    }
    @Test void commentAndReplyQueueInspectionWhileApprovedFeedDoesNotQueueAgain() {
        var principal=new UserPrincipal(user,"test");
        var commentRequest=new CreateCommentRequest();commentRequest.setContent("new comment");
        var createdComment=feeds.createComment(principal,feed,commentRequest);
        assertThat(mapper.findJob(job("COMMENT",createdComment.getFeedCommentId(),1)).getStatus()).isEqualTo("PENDING");
        var replyRequest=new CreateReplyRequest();replyRequest.setContent("new reply");
        var createdReply=feeds.createReply(principal,feed,comment,replyRequest);
        assertThat(mapper.findJob(job("REPLY",createdReply.getFeedReplyId(),1)).getStatus()).isEqualTo("PENDING");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES(?,?,?,?,?,?)",
                "moderation-test.jpg","images/moderation-test.jpg","image/jpeg","jpg","OWNED",user);
        Long file=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        var media=new CreateFeedRequest.MediaItem();media.setFileId(file);media.setMediaType("IMAGE");
        var request=new CreateFeedRequest();request.setContent("new feed");request.setMedia(java.util.List.of(media));
        var createdFeed=feeds.create(principal,request);
        assertThat(createdFeed.getContent()).isEqualTo("new feed");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_ai_moderation_job WHERE target_type='FEED' AND target_id=?",Integer.class,createdFeed.getFeedId())).isZero();
        assertThat(mapper.lockState("FEED",createdFeed.getFeedId()).isHasApproved()).isTrue();
        assertThat(mapper.lockState("FEED",createdFeed.getFeedId()).getApprovedContent()).isEqualTo("new feed");
    }
    @Test void rejectedEditRestoresApprovedContentAndPersistsOneNotice() {
        write("unsafe","original",false);var j=claim(1);
        transactions.complete(j,unsafe());transactions.complete(j,unsafe());
        assertThat(content()).isEqualTo("original");
        assertThat(mapper.findJob(j.getJobId()).getResultAction()).isEqualTo("RESTORED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND group_key=?",Integer.class,user,"AI_MODERATION:"+j.getJobId())).isEqualTo(1);
    }
    @Test void rejectedCreateIsHiddenWithoutDestroyingContentAndCountIsAdjustedOnce() {
        write("unsafe",null,true);var j=claim(1);
        transactions.complete(j,unsafe());transactions.complete(j,unsafe());
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT comment_count FROM ultary_feed WHERE feed_id=?",Integer.class,feed)).isZero();
        assertThat(jdbc.queryForObject("SELECT feed_id FROM ultary_notification WHERE group_key=?",Long.class,"AI_MODERATION:"+j.getJobId())).isNull();
    }
    @Test void oldRejectionCannotUndoANewerEdit() {
        write("B","original",false);var old=claim(1);write("C","B",false);
        transactions.complete(old,unsafe());
        assertThat(content()).isEqualTo("C");assertThat(mapper.findJob(old.getJobId()).getStatus()).isEqualTo("SUPERSEDED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?",Integer.class,user)).isZero();
        transactions.complete(claim(2),unsafe());assertThat(content()).isEqualTo("original");
    }
    @Test void aSafeEarlierRevisionBecomesTheRollbackBaselineForTheLatestRevision() {
        write("B","original",false);var old=claim(1);write("C","B",false);
        transactions.complete(old,ModerationResult.allowed());
        transactions.complete(claim(2),unsafe());assertThat(content()).isEqualTo("B");
    }
    @Test void failureRetriesWithoutTouchingContentAndEventuallyRemainsFailed() {
        write("keep","original",false);
        var failure=new ModerationResult(null,null,"GEMINI_TIMEOUT","Read timed out",true);
        for(int attempt=1;attempt<=3;attempt++) {
            var j=claim(1);assertThat(j.getAttemptCount()).isEqualTo(attempt);transactions.complete(j,failure);
            assertThat(mapper.findJob(j.getJobId()).getStatus()).isEqualTo(attempt<3?"PENDING":"FAILED");
            jdbc.update("UPDATE ultary_ai_moderation_job SET next_attempt_at=NOW() WHERE job_id=?",j.getJobId());
        }
        assertThat(content()).isEqualTo("keep");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?",Integer.class,user)).isZero();
    }
    @Test void expiredClaimsAreRecoveredAndTheOldWorkerCannotApplyAResult() {
        write("keep","original",false);var old=claim(1);
        assertThat(transactions.claim(old.getJobId())).isNull();
        jdbc.update("UPDATE ultary_ai_moderation_job SET lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE job_id=?",old.getJobId());
        assertThat(mapper.findReady(100)).contains(old.getJobId());
        var recovered=transactions.claim(old.getJobId());
        assertThat(recovered.getClaimToken()).isNotEqualTo(old.getClaimToken());
        transactions.complete(old,unsafe());assertThat(content()).isEqualTo("keep");
        transactions.complete(recovered,ModerationResult.allowed());
        assertThat(mapper.findJob(old.getJobId()).getStatus()).isEqualTo("APPROVED");
    }
    @Test void deletionWhileWaitingIsNeverResurrectedByARejection() {
        write("keep","original",false);var j=claim(1);
        jdbc.update("UPDATE ultary_feed_comment SET is_deleted=1 WHERE feed_comment_id=?",comment);
        transactions.complete(j,unsafe());assertThat(mapper.findJob(j.getJobId()).getStatus()).isEqualTo("SKIPPED");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isEqualTo(1);
    }
    @Test void newFeedAndReplyTargetsUseTheSameDurableDecisionFlow() {
        queue.enqueue("FEED",feed,feed,user,null,"fixture",true);
        var f=transactions.claim(job("FEED",feed,1));transactions.complete(f,ModerationResult.allowed());
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,?)",comment,user,"reply");
        Long reply=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        queue.enqueue("REPLY",reply,feed,user,null,"reply",true);
        var r=transactions.claim(job("REPLY",reply,1));transactions.complete(r,unsafe());
        assertThat(mapper.findJob(f.getJobId()).getStatus()).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_reply WHERE feed_reply_id=?",Integer.class,reply)).isEqualTo(1);
    }
}
