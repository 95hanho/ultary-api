package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import me._hanho.ultary.domain.ai.model.AiRequestLog;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
class AiLogIntegrationTest {
    @Autowired AiLogService logs;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Test void serviceFailureLogSurvivesOuterRollback() {
        Long user=jdbc.queryForObject("SELECT MIN(user_no) FROM ultary_user WHERE withdrawal_status='ACTIVE'",Long.class);
        assertThat(user).isNotNull();
        var row=new AiRequestLog();row.setUserNo(user);row.setFeatureType("COMMENT_FILTER");row.setStatus("FAILED");
        row.setErrorCode("GEMINI_TIMEOUT");row.setErrorMessage("integration test; no provider request");
        try {
            new TransactionTemplate(manager).execute(status->{logs.write(row);status.setRollbackOnly();return null;});
            assertThat(jdbc.queryForObject("SELECT error_code FROM ultary_ai_request_log WHERE ai_request_id=?",String.class,row.getAiRequestId())).isEqualTo("GEMINI_TIMEOUT");
        } finally {
            if(row.getAiRequestId()!=null)jdbc.update("DELETE FROM ultary_ai_request_log WHERE ai_request_id=?",row.getAiRequestId());
        }
    }
}
