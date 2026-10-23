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
    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
