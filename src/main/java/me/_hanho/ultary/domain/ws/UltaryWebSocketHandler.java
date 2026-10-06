package me._hanho.ultary.domain.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UltaryWebSocketHandler extends TextWebSocketHandler {

	private static final String AUTHED = "wsAuthed";
	private static final String TIMEOUT = "wsAuthTimeout";

	private final WsTicketService ticketService;
	private final WsSessionRegistry registry;
	private final RealtimePush realtimePush;
	private final ObjectMapper objectMapper;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, "ws-auth-timeout");
		thread.setDaemon(true);
		return thread;
	});

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		ScheduledFuture<?> timeout = scheduler.schedule(() -> {
			if (!Boolean.TRUE.equals(session.getAttributes().get(AUTHED))) {
				closeQuietly(session);
			}
		}, 5, TimeUnit.SECONDS);
		session.getAttributes().put(TIMEOUT, timeout);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		if (Boolean.TRUE.equals(session.getAttributes().get(AUTHED))) {
			return;
		}
		JsonNode body;
		try {
			body = objectMapper.readTree(message.getPayload());
		} catch (IOException ex) {
			closeQuietly(session);
			return;
		}
		if (!"AUTH".equals(body.path("type").asText())) {
			closeQuietly(session);
			return;
		}
		Long userNo = ticketService.consume(body.path("ticket").asText(null));
		if (userNo == null) {
			closeQuietly(session);
			return;
		}
		cancelTimeout(session);
		session.getAttributes().put(AUTHED, true);
		registry.add(userNo, session);
		send(session, Map.of("type", "AUTH_OK"));
		realtimePush.pushNotificationUnread(userNo);
		log.info("[ws] open userNo={}", userNo);
	}

	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		cancelTimeout(session);
		registry.remove(session);
	}

	private void cancelTimeout(WebSocketSession session) {
		Object timeout = session.getAttributes().get(TIMEOUT);
		if (timeout instanceof ScheduledFuture<?> future) {
			future.cancel(false);
		}
	}

	private void send(WebSocketSession session, Object payload) throws IOException {
		synchronized (session) {
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
		}
	}

	private void closeQuietly(WebSocketSession session) {
		try {
			session.close(CloseStatus.POLICY_VIOLATION);
		} catch (IOException ex) {
			log.info("[ws] close 실패");
		}
	}
}
