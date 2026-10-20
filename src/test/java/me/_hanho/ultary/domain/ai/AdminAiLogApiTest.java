package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import com.fasterxml.jackson.databind.ObjectMapper;
import me._hanho.ultary.domain.ai.model.AiRequestLog;
import me._hanho.ultary.security.jwt.JwtTokenProvider;

/** Exercises the real admin JWT filter and MariaDB mapper; fixtures roll back. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class AdminAiLogApiTest {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired JdbcTemplate jdbc;
    @Autowired AiRequestLogMapper logs;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
    @Autowired JwtTokenProvider tokens;
    @Autowired ObjectMapper json;
    MockMvc mvc;
    Long user, first, second, success;
    int originalFailureCount;
    String adminToken;
    final String path = "/api/v1/admin/ai/logs";

    @BeforeEach void fixture() {
        originalFailureCount = jdbc.queryForObject("SELECT COUNT(*) FROM ultary_ai_request_log WHERE status='FAILED'", Integer.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "ailog" + UUID.randomUUID().toString().substring(0, 12));
        user = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ultary_admin(login_id,password) VALUES(?,?)", "ailog-" + UUID.randomUUID(), "unused-test-password");
        adminToken = tokens.createAdminAccessToken(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        first = insert("FAILED", "GEMINI_TIMEOUT");
        second = insert("FAILED", "GEMINI_HTTP_429");
        success = insert("SUCCESS", null);
    }

    Long insert(String status, String code) {
        var row = new AiRequestLog();
        row.setUserNo(user); row.setFeatureType("COMMENT_FILTER"); row.setTargetType("COMMENT"); row.setTargetId(120L);
        row.setStatus(status); row.setErrorCode(code); row.setAttemptNo(1); row.setLatencyMs(8000L);
        row.setHttpStatus(code != null && code.endsWith("429") ? 429 : null);
        row.setPrompt("not exposed"); row.setResult("not exposed");
        logs.insert(row); return row.getAiRequestId();
    }

    @Test void menuBadgeCountsFailedAttemptsAndExcludesSuccessfulCalls() throws Exception {
        String menu = "/api/v1/admin/menu-counts";
        mvc.perform(get(menu).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.failedAiRequestCount").value(originalFailureCount + 2));
        jdbc.update("UPDATE ultary_ai_request_log SET status='SUCCESS' WHERE ai_request_id=?", first);
        // JDBC fixture changes bypass MyBatis' transaction-scoped first-level cache.
        sqlSession.clearCache();
        mvc.perform(get(menu).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.failedAiRequestCount").value(originalFailureCount + 1));
    }

    @Test void onlyAdminsCanReadListAndDetail() throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "/" + first)).andExpect(status().isUnauthorized());
        String userToken = tokens.createUserAccessToken(user);
        mvc.perform(get(path).header("Authorization", "Bearer " + userToken)).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "/" + first).header("Authorization", "Bearer " + userToken)).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "/" + first).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.errorCode").value("GEMINI_TIMEOUT"))
                .andExpect(jsonPath("$.data.prompt").doesNotExist()).andExpect(jsonPath("$.data.result").doesNotExist());
    }

    @Test void defaultsToFailuresAndPaginatesWithoutDuplicates() throws Exception {
        String response = mvc.perform(get(path).header("Authorization", "Bearer " + adminToken)
                .param("userNo", user.toString()).param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].aiRequestId").value(second))
                .andReturn().getResponse().getContentAsString();
        long cursor = json.readTree(response).path("data").path("nextCursorAiRequestId").asLong();
        assertThat(cursor).isEqualTo(second);
        mvc.perform(get(path).header("Authorization", "Bearer " + adminToken).param("userNo", user.toString())
                .param("limit", "1").param("cursorAiRequestId", Long.toString(cursor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].aiRequestId").value(first))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.nextCursorAiRequestId").isEmpty());
    }

    @Test void filtersAndAllStatusAreSupported() throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + adminToken).param("userNo", user.toString())
                .param("status", "ALL").param("featureType", "COMMENT_FILTER").param("targetType", "COMMENT").param("targetId", "120"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.totalCount").value(3));
        mvc.perform(get(path).header("Authorization", "Bearer " + adminToken).param("userNo", user.toString())
                .param("errorCode", "GEMINI_HTTP_429"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].httpStatus").value(429));
    }

    @Test void invalidInputsAndMissingDetailReturnProperErrors() throws Exception {
        for (String query : new String[]{"limit=0", "limit=101", "cursorAiRequestId=-1", "status=BAD", "targetType=SQL", "targetId=0", "errorCode=bad"}) {
            mvc.perform(get(path + "?" + query).header("Authorization", "Bearer " + adminToken)).andExpect(status().isBadRequest());
        }
        mvc.perform(get(path + "/9223372036854775807").header("Authorization", "Bearer " + adminToken)).andExpect(status().isNotFound());
    }

    @Test void attemptHistoryIncludesTheCurrentJobOutcome() throws Exception {
        jdbc.update("INSERT INTO ultary_ai_moderation_job(target_type,target_id,feed_id,user_no,content_version,operation,status,attempt_count,result_action) VALUES('COMMENT',120,120,?,1,'UPDATE','REJECTED',2,'RESTORED')", user);
        Long job = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE ultary_ai_request_log SET moderation_job_id=?,content_version=1 WHERE ai_request_id IN (?,?)", job, first, second);
        mvc.perform(get(path).header("Authorization", "Bearer " + adminToken).param("moderationJobId", job.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.items[0].jobStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.items[0].jobAttemptCount").value(2))
                .andExpect(jsonPath("$.data.items[0].resultAction").value("RESTORED"));
    }
}
