package me._hanho.ultary.domain.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
@EnableScheduling
public class AiConfig {
	@Bean
	public ThreadPoolTaskExecutor moderationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("ai-moderation-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		return executor;
	}
}
