package me._hanho.ultary.domain.ws;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@EnableScheduling
@EnableConfigurationProperties(WsProperties.class)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

	private final UltaryWebSocketHandler handler;
	private final WsProperties properties;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler, "/api/v1/ws")
				.setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
	}

	@Bean
	public ServletServerContainerFactoryBean webSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(64 * 1024);
		return container;
	}
}
