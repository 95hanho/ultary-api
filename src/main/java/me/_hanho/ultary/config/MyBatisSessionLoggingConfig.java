package me._hanho.ultary.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;

/** Keep polling session lifecycle noise quiet, including DevTools context restarts. */
@Configuration
public class MyBatisSessionLoggingConfig {
    @EventListener(ApplicationReadyEvent.class)
    public void configureSessionLogging() {
        LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
        logging.setLogLevel("org.mybatis.spring.SqlSessionUtils", LogLevel.INFO);
        logging.setLogLevel("org.mybatis.spring.transaction.SpringManagedTransaction", LogLevel.INFO);
    }
}
