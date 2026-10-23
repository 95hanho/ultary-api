package me._hanho.ultary.domain.admin;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.bff.BffSecurityProperties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ai.moderation.worker-enabled=false")
@AutoConfigureMockMvc
@Transactional
class AdminReportPreviewIntegrationTest {
    @Autowired AdminService admin;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired BffSecurityProperties bff;
    Long author, reporter, feed, comment, reply;

    @BeforeEach void setup() {
        author = user(); reporter = user();
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'private original','PRIVATE')", author); feed = id();
        jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'comment original')", feed, author); comment = id();
        jdbc.update("INSERT INTO ultary_feed_reply(feed_comment_id,user_no,content) VALUES(?,?,'reply original')", comment, author); reply = id();
    }

    @Test void resolvesFeedForEachReportTypeAndKeepsTheReportedDeletedThread() {
        Long feedReport = report("FEED"), commentReport = report("COMMENT"), replyReport = report("REPLY");
        assertThat(admin.reportPreview(feedReport).feed().getFeedId()).isEqualTo(feed);
        assertThat(admin.reportPreview(commentReport).comments().get(0).getFeedCommentId()).isEqualTo(comment);
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?", feed);
        jdbc.update("UPDATE ultary_feed_comment SET is_deleted=1 WHERE feed_comment_id=?", comment);
        jdbc.update("UPDATE ultary_feed_reply SET is_deleted=1 WHERE feed_reply_id=?", reply);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='SUSPENDED' WHERE user_no=?", author);
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,created_by_user_no,is_deleted) VALUES(?,'USER',?,1)", "deleted_" + author, author);
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)", feed, id());
        var preview = admin.reportPreview(replyReport);
        assertThat(preview.feed().getContent()).isEqualTo("private original");
        assertThat(preview.feed().getAuthorNickname()).isNotBlank();
        assertThat(preview.comments().get(0).getReplies().get(0).getContent()).isEqualTo("reply original");
    }

    @Test void requiresAdminTokenAndNeverMutatesTheReportOnPreview() throws Exception {
        Long report = report("REPLY");
        String url = "/api/v1/admin/reports/" + report + "/preview";
        mvc.perform(get(url).secure(true).header("X-BFF-Key", bff.getSharedKey())).andExpect(status().isUnauthorized());
        mvc.perform(get(url).secure(true).header("X-BFF-Key", bff.getSharedKey()).header("Authorization", "Bearer " + jwt.createUserAccessToken(reporter)))
            .andExpect(status().isUnauthorized());
        Long adminNo = jdbc.queryForObject("SELECT MIN(admin_no) FROM ultary_admin WHERE status='ACTIVE'", Long.class);
        mvc.perform(get(url).secure(true).header("X-BFF-Key", bff.getSharedKey()).header("Authorization", "Bearer " + jwt.createAdminAccessToken(adminNo)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.feed.feedId").value(feed))
            .andExpect(jsonPath("$.data.comments[0].replies[0].content").value("reply original"));
        assertThat(jdbc.queryForObject("SELECT status FROM ultary_report WHERE report_id=?", String.class, report)).isEqualTo("REQUESTED");
    }

    @Test void nonexistentReportIsNotAnUnrestrictedFeedLookup() {
        assertThatThrownBy(() -> admin.reportPreview(Long.MAX_VALUE)).isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND));
    }
    @Test void previewFilesAreLimitedToTheReportedFeedAndThread() {
        Long report = report("FEED");
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('preview.jpg','images/preview.jpg','image/jpeg','jpg','OWNED',?)", author);
        Long file = id();
        jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)", feed, file);
        assertThatCode(() -> admin.assertReportPreviewFile(report, file)).doesNotThrowAnyException();
        assertThatThrownBy(() -> admin.assertReportPreviewFile(report, Long.MAX_VALUE)).isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
    }
    Long report(String type) {
        jdbc.update("INSERT INTO ultary_report(reporter_user_no,target_type,target_feed_id,target_comment_id,target_reply_id,reason) VALUES(?,?,?,?,?,'ABUSE')",
            reporter, type, "FEED".equals(type) ? feed : null, "COMMENT".equals(type) ? comment : null, "REPLY".equals(type) ? reply : null);
        return id();
    }
    Long user() { jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "preview_" + UUID.randomUUID().toString().substring(0, 8)); return id(); }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
}
