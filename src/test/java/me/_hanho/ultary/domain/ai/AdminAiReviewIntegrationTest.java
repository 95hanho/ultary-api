package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.admin.AdminAiLogService;
import me._hanho.ultary.domain.ai.model.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class AdminAiReviewIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ModerationQueue queue;
    @Autowired ModerationMapper mapper;
    @Autowired ModerationTransactions transactions;
    @Autowired AiRequestLogMapper logs;
    @Autowired AdminAiLogService adminLogs;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
    @Autowired me._hanho.ultary.domain.notification.NotificationMapper notices;
    Long user, admin, feed, comment, reply;
    @BeforeEach void setup() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "manual_"+UUID.randomUUID().toString().substring(0,8)); user=id();
        admin=jdbc.queryForObject("SELECT MIN(admin_no) FROM ultary_admin",Long.class);
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility,comment_count) VALUES(?,'current','PUBLIC',1)",user);feed=id();
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'current')",feed,user);comment=id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'current')",comment,user);reply=id();
    }
    Long error(String kind, Long target) {
        queue.enqueue(kind,target,feed,user,null,"current",true);
        Long job=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE user_no=?",Long.class,user);
        var log=new AiRequestLog();log.setUserNo(user);log.setFeatureType("COMMENT_FILTER");log.setStatus("FAILED");log.setErrorCode("GEMINI_TIMEOUT");
        log.setModerationJobId(job);log.setTargetType(kind);log.setTargetId(target);log.setTargetFeedId(feed);log.setContentVersion(1L);log.setAttemptNo(1);
        logs.insert(log);return log.getAiRequestId();
    }
    @Test void failedContentIsVisibleUntilManualReviewAndAnInFlightWorkerCannotOverrideIt() {
        Long log=error("COMMENT",comment); var detail=adminLogs.detail(log);var claimed=transactions.claim(detail.getModerationJobId());
        assertThat(detail.getInspectionContent()).isEqualTo("current");
        assertThat(adminLogs.list("FAILED",null,"MODERATION",null,null,user,null,null,30).items().get(0).getInspectionContent()).isEqualTo("current");
        adminLogs.review(log,admin,"KEPT");
        transactions.complete(claimed,new ModerationResult(false,"ABUSE",null,null,false));
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isZero();
        assertThat(mapper.findJob(claimed.getJobId()).getStatus()).isEqualTo("SKIPPED");
        adminLogs.review(log,admin,"DELETED");adminLogs.review(log,admin,"DELETED");
        assertThat(jdbc.queryForObject("SELECT comment_count FROM ultary_feed WHERE feed_id=?",Integer.class,feed)).isZero();
        assertThat(adminLogs.detail(log).getAdminReviewStatus()).isEqualTo("DELETED");
    }
    @Test void eachTargetCanBeRemovedAndTheGroupedFilterExcludesRecommendations() {
        Long feedLog=error("FEED",feed), replyLog=error("REPLY",reply);
        adminLogs.review(replyLog,admin,"DELETED");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_reply WHERE feed_reply_id=?",Integer.class,reply)).isEqualTo(1);
        adminLogs.review(feedLog,admin,"DELETED");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed WHERE feed_id=?",Integer.class,feed)).isEqualTo(1);
        var recommendation=new AiRequestLog();recommendation.setUserNo(user);recommendation.setFeatureType("HASHTAG");recommendation.setStatus("FAILED");logs.insert(recommendation);
        assertThat(adminLogs.list("FAILED",null,"MODERATION",null,null,user,null,null,30).totalCount()).isEqualTo(2);
    }
    @Test void preSaveFailureRetainsInspectionTextWithoutAJobOrSavedFeed() {
        var row=new AiRequestLog();row.setUserNo(user);row.setFeatureType("FEED_CAPTION");
        row.setStatus("FAILED");row.setTargetType("FEED");row.setErrorCode("GEMINI_TIMEOUT");row.setInspectionContent("unsaved draft");
        logs.insert(row);
        var result=adminLogs.list("FAILED",null,"MODERATION",null,null,user,null,null,30).items().get(0);
        assertThat(result.getInspectionContent()).isEqualTo("unsaved draft");
        assertThat(result.getModerationJobId()).isNull();
        assertThatThrownBy(()->adminLogs.review(row.getAiRequestId(),admin,"DELETED")).isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
    }
    @Test void oldErrorCannotActOnANewerContentVersion() {
        Long log=error("COMMENT",comment);
        jdbc.update("UPDATE ultary_feed_comment SET content='newer' WHERE feed_comment_id=?",comment);
        queue.enqueue("COMMENT",comment,feed,user,"current","newer",false);
        assertThatThrownBy(()->adminLogs.review(log,admin,"DELETED")).isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isZero();
    }

    @Test void enqueueRejectsMismatchedFeedOwnerOrSnapshotBeforeCreatingAJob() {
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'other','PUBLIC')",user);
        Long otherFeed=id();
        assertThatThrownBy(()->queue.enqueue("COMMENT",comment,otherFeed,user,null,"current",true))
            .hasMessageContaining("게시글 연결 정보");
        assertThatThrownBy(()->queue.enqueue("REPLY",reply,otherFeed,user,null,"current",true))
            .hasMessageContaining("게시글 연결 정보");
        assertThatThrownBy(()->queue.enqueue("COMMENT",comment,feed,user+1,null,"current",true))
            .hasMessageContaining("작성자 또는 본문");
        assertThatThrownBy(()->queue.enqueue("COMMENT",comment,feed,user,null,"different",true))
            .hasMessageContaining("작성자 또는 본문");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_ai_moderation_job WHERE user_no=?",Integer.class,user)).isZero();
    }

    @Test void mismatchedRecordedFeedIsExplainedAndAlignedMetadataAllowsReview() {
        Long log=error("COMMENT",comment);
        Long job=adminLogs.detail(log).getModerationJobId();
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility,is_deleted) VALUES(?,'old','PUBLIC',1)",user);
        Long oldFeed=id();
        jdbc.update("UPDATE ultary_ai_moderation_job SET feed_id=? WHERE job_id=?",oldFeed,job);
        jdbc.update("UPDATE ultary_ai_request_log SET target_feed_id=? WHERE moderation_job_id=?",oldFeed,job);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"DELETED")).hasMessageContaining("게시글 연결 정보");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isZero();
        jdbc.update("UPDATE ultary_ai_moderation_job SET feed_id=? WHERE job_id=?",feed,job);
        jdbc.update("UPDATE ultary_ai_request_log SET target_feed_id=? WHERE moderation_job_id=?",feed,job);
        sqlSession.clearCache();
        adminLogs.review(log,admin,"DELETED");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT comment_count FROM ultary_feed WHERE feed_id=?",Integer.class,feed)).isZero();
    }

    @Test void deletedParentsAndDeletedTargetsHaveDistinctErrors() {
        Long log=error("COMMENT",comment);
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?",feed);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"DELETED")).hasMessageContaining("부모 게시글");
        jdbc.update("UPDATE ultary_feed SET is_deleted=0 WHERE feed_id=?",feed);
        jdbc.update("UPDATE ultary_feed_comment SET is_deleted=1 WHERE feed_comment_id=?",comment);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"DELETED")).hasMessageContaining("콘텐츠가 없거나 삭제");
    }

    @Test void deletedParentCommentPreventsReplyReviewWithASpecificError() {
        Long log=error("REPLY",reply);
        jdbc.update("UPDATE ultary_feed_comment SET is_deleted=1 WHERE feed_comment_id=?",comment);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"DELETED")).hasMessageContaining("부모 댓글");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_reply WHERE feed_reply_id=?",Integer.class,reply)).isZero();
    }

    @Test void deletionCanBeReversedToKeptOrPendingForEveryTargetWithoutDoubleCounting() {
        String[] kinds={"FEED","COMMENT","REPLY"};
        Long[] targets={feed,comment,reply};
        String[] tables={"ultary_feed","ultary_feed_comment","ultary_feed_reply"};
        String[] keys={"feed_id","feed_comment_id","feed_reply_id"};
        for (int i=0; i<kinds.length; i++) {
            Long log=error(kinds[i],targets[i]);
            for (String restoredStatus : new String[]{"KEPT","PENDING"}) {
                adminLogs.review(log,admin,"DELETED");
                assertThat(jdbc.queryForObject("SELECT is_deleted FROM " + tables[i] + " WHERE " + keys[i] + "=?",Integer.class,targets[i])).isEqualTo(1);
                adminLogs.review(log,admin,restoredStatus);
                adminLogs.review(log,admin,restoredStatus);
                assertThat(jdbc.queryForObject("SELECT is_deleted FROM " + tables[i] + " WHERE " + keys[i] + "=?",Integer.class,targets[i])).isZero();
                assertThat(jdbc.queryForObject("SELECT comment_count FROM ultary_feed WHERE feed_id=?",Integer.class,feed)).isEqualTo(1);
                assertThat(adminLogs.detail(log).getAdminReviewStatus()).isEqualTo(restoredStatus);
                assertThat(adminLogs.detail(log).getJobStatus()).isEqualTo("SKIPPED");
            }
        }
    }

    @Test void reversingADeletionStillChecksContentVersionAndDeletedParents() {
        Long log=error("COMMENT",comment);
        adminLogs.review(log,admin,"DELETED");
        jdbc.update("UPDATE ultary_ai_moderation_state SET latest_version=latest_version+1 WHERE target_type='COMMENT' AND target_id=?",comment);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"KEPT")).hasMessageContaining("검사 이후 콘텐츠");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isEqualTo(1);
        jdbc.update("UPDATE ultary_ai_moderation_state SET latest_version=latest_version-1 WHERE target_type='COMMENT' AND target_id=?",comment);
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?",feed);
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"PENDING")).hasMessageContaining("부모 게시글");
        assertThat(adminLogs.detail(log).getAdminReviewStatus()).isEqualTo("DELETED");
    }

    @Test void anActiveReportDeletionPreventsAccidentalRestoration() {
        Long log=error("COMMENT",comment);
        adminLogs.review(log,admin,"DELETED");
        jdbc.update("INSERT INTO ultary_report(reporter_user_no,target_type,target_comment_id,reason,status) VALUES(?,'COMMENT',?,'ABUSE','DELETED')",user,comment);
        Long report=id();
        sqlSession.clearCache();
        assertThatThrownBy(()->adminLogs.review(log,admin,"KEPT")).hasMessageContaining("신고에서 삭제조치");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isEqualTo(1);
        jdbc.update("UPDATE ultary_report SET status='REJECTED' WHERE report_id=?",report);
        sqlSession.clearCache();
        adminLogs.review(log,admin,"KEPT");
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM ultary_feed_comment WHERE feed_comment_id=?",Integer.class,comment)).isZero();
    }

    @Test void everyAdminChangeCreatesAPersistentNoticeWithTheCorrectTargetEvenWhenHidden() {
        String[] kinds={"FEED","COMMENT","REPLY"};
        Long[] targets={feed,comment,reply};
        String[] nouns={"게시글","댓글","답글"};
        for (int i=0; i<kinds.length; i++) {
            String noun=nouns[i];
            Long log=error(kinds[i],targets[i]);
            Long job=adminLogs.detail(log).getModerationJobId();
            String group="AI_MODERATION:"+job+":%";
            adminLogs.review(log,admin,"DELETED");
            var hiddenNotice=notices.findByReceiver(user,50).stream()
                    .filter(row->row.getContent().equals("관리자 확인 후 "+noun+"이 숨김 처리되었습니다."))
                    .findFirst().orElseThrow();
            assertThat(hiddenNotice.getFeedId()).isEqualTo(feed);
            assertThat(hiddenNotice.getFeedCommentId()).isEqualTo(i==0 ? null : comment);
            assertThat(hiddenNotice.getFeedReplyId()).isEqualTo(i==2 ? reply : null);
            assertThat(notices.findByIdAndReceiver(hiddenNotice.getNotificationId(),user).getFeedId()).isEqualTo(feed);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND group_key LIKE ? AND feed_id IS NULL AND feed_comment_id IS NULL AND feed_reply_id IS NULL",Integer.class,user,group)).isEqualTo(1);
            adminLogs.review(log,admin,"KEPT");
            adminLogs.review(log,admin,"PENDING");
            adminLogs.review(log,admin,"PENDING");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND group_key LIKE ?",Integer.class,user,group)).isEqualTo(3);
            assertThat(notices.findByReceiver(user,50).stream().anyMatch(row->row.getContent().contains(noun+"의 삭제조치가 취소되어 복구"))).isTrue();
        }
    }

    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
