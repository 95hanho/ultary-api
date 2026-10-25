package me._hanho.ultary.domain.report;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.admin.AdminService;

/** Real target-owner joins; all fixture users, content and reports roll back. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class ReportMenuCountIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ReportMapper reports;
    @Autowired AdminService admin;
    @Autowired SqlSessionTemplate sqlSession;

    @Test void badgeUsesTheSameActionableTargetsAsTheListIncludingSuspensionChanges() {
        int baseline = reports.countOpen();
        Long reporter = user("ACTIVE");
        Long active = user("ACTIVE");
        Long suspended = user("SUSPENDED");
        fixtures(reporter, active, false);
        fixtures(reporter, suspended, true);
        sqlSession.clearCache();
        assertThat(admin.menuCounts().getRequestedReportCount()).isEqualTo(baseline + 4);

        jdbc.update("UPDATE ultary_user SET withdrawal_status='SUSPENDED' WHERE user_no=?", active);
        sqlSession.clearCache();
        assertThat(admin.menuCounts().getRequestedReportCount()).isEqualTo(baseline);

        jdbc.update("UPDATE ultary_user SET withdrawal_status='ACTIVE' WHERE user_no=?", active);
        sqlSession.clearCache();
        assertThat(admin.menuCounts().getRequestedReportCount()).isEqualTo(baseline + 4);
    }

    Long user(String status) {
        jdbc.update("INSERT INTO ultary_user(nickname,withdrawal_status) VALUES(?,?)", "rpt" + UUID.randomUUID().toString().substring(0,12), status);
        return id();
    }

    void fixtures(Long reporter, Long owner, boolean suspended) {
        String reporterNickname = jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?", String.class, reporter);
        String ownerNickname = jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?", String.class, owner);
        jdbc.update("INSERT INTO ultary_feed(user_no,content) VALUES(?,'fixture')", owner);
        Long feed = id();
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'fixture')", feed, owner);
        Long comment = id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'fixture')", comment, owner);
        Long reply = id();
        String[] types = {"USER", "FEED", "COMMENT", "REPLY"};
        String[] statuses = {"REQUESTED", "CONFIRMED", "REQUESTED", "CONFIRMED"};
        for (int i=0; i<types.length; i++) {
            jdbc.update("INSERT INTO ultary_report(reporter_user_no,target_type,target_user_no,target_feed_id,target_comment_id,target_reply_id,reason,status) VALUES(?,?,?,?,?,?,'SPAM',?)",
                    reporter, types[i], i == 0 ? owner : null, i == 1 ? feed : null, i == 2 ? comment : null, i == 3 ? reply : null, statuses[i]);
            var report = reports.findAdminById(id());
            assertThat(report.isTargetSuspended()).isEqualTo(suspended);
            assertThat(report.getReporterUserNo()).isEqualTo(reporter);
            assertThat(report.getReporterNickname()).isEqualTo(reporterNickname);
            assertThat(report.getTargetUserNo()).isEqualTo(owner);
            assertThat(report.getTargetNickname()).isEqualTo(ownerNickname);
        }
        for (String closed : new String[]{"DELETED", "REJECTED", "SUSPENDED"}) {
            jdbc.update("INSERT INTO ultary_report(reporter_user_no,target_type,target_user_no,reason,status) VALUES(?,'USER',?,'SPAM',?)", reporter, owner, closed);
        }
    }

    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
