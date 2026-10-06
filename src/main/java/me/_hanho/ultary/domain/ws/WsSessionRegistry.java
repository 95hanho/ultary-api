package me._hanho.ultary.domain.ws;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsSessionRegistry {

	static final String USER_NO = "wsUserNo";

	private final ObjectMapper objectMapper;
	private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

	public void add(Long userNo, WebSocketSession session) {
		session.getAttributes().put(USER_NO, userNo);
		sessions.computeIfAbsent(userNo, key -> ConcurrentHashMap.newKeySet()).add(session);
	}

	public void remove(WebSocketSession session) {
		Object userNo = session.getAttributes().get(USER_NO);
		if (!(userNo instanceof Long id)) {
			return;
		}
		Set<WebSocketSession> mine = sessions.get(id);
		if (mine == null) {
			return;
		}
		mine.remove(session);
		if (mine.isEmpty()) {
			sessions.remove(id, mine);
		}
	}

	public void send(Long userNo, Object payload) {
		Set<WebSocketSession> mine = sessions.get(userNo);
		if (mine == null || mine.isEmpty()) {
			return;
		}
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (IOException ex) {
			log.warn("[ws] json 실패 userNo={}", userNo);
			return;
		}
		TextMessage message = new TextMessage(json);
		for (WebSocketSession session : mine) {
			if (!session.isOpen()) {
				remove(session);
				continue;
			}
			try {
				synchronized (session) {
					session.sendMessage(message);
				}
			} catch (IOException ex) {
				log.info("[ws] 전송 실패 userNo={}", userNo);
				remove(session);
			}
		}
	}

	@Scheduled(fixedRate = 25_000)
	public void ping() {
		for (Long userNo : sessions.keySet()) {
			send(userNo, java.util.Map.of("type", "PING"));
		}
	}
}
