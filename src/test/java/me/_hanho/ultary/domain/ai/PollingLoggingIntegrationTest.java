package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import java.time.Duration;

/** Real non-transactional polling stays quiet even after a development context restart. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@ExtendWith(OutputCaptureExtension.class)
class PollingLoggingIntegrationTest {
    @Autowired ModerationMapper mapper;
    @Autowired ApplicationEventPublisher events;
    @Autowired ConfigurableApplicationContext context;

    @Test void readyEventRestoresQuietSessionLoggingWithoutDisablingSqlOrErrors(CapturedOutput output) {
        LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
        String sessions = "org.mybatis.spring.SqlSessionUtils";
        String transactions = "org.mybatis.spring.transaction.SpringManagedTransaction";
        logging.setLogLevel(sessions, LogLevel.DEBUG);
        logging.setLogLevel(transactions, LogLevel.DEBUG);
        LogLevel sqlLevel = logging.getLoggerConfiguration("org.apache.ibatis").getEffectiveLevel();
        events.publishEvent(new ApplicationReadyEvent(new org.springframework.boot.SpringApplication(), new String[0], context, Duration.ZERO));
        assertThat(logging.getLoggerConfiguration(sessions).getEffectiveLevel()).isEqualTo(LogLevel.INFO);
        assertThat(logging.getLoggerConfiguration(transactions).getEffectiveLevel()).isEqualTo(LogLevel.INFO);
        assertThat(logging.getLoggerConfiguration("org.apache.ibatis").getEffectiveLevel()).isEqualTo(sqlLevel);
        int outputStart = output.getOut().length();
        mapper.findReady(2);
        mapper.findReady(2);
        assertThat(output.getOut().substring(outputStart)).doesNotContain("Creating a new SqlSession", "was not registered for synchronization", "will not be managed by Spring", "Closing non transactional SqlSession");
    }
}
