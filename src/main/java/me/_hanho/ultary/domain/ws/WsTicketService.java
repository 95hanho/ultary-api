package me._hanho.ultary.domain.ws;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.ws.dto.WsTicketResponse;

@Service
@RequiredArgsConstructor
public class WsTicketService {

	private final WsProperties properties;
	private final SecureRandom random = new SecureRandom();
	private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

	public WsTicketResponse issue(Long userNo) {
		evictExpired();
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		int ttl = Math.max(properties.getTicketTtlSeconds(), 5);
		tickets.put(value, new Ticket(userNo, Instant.now().plusSeconds(ttl)));
		return WsTicketResponse.builder()
				.ticket(value)
				.expiresIn(ttl)
				.build();
	}

	/** 맞으면 사용자 번호를 반환하고 티켓을 버린다. 틀리면 null */
	public Long consume(String ticket) {
		if (ticket == null || ticket.isBlank()) {
			return null;
		}
		Ticket found = tickets.remove(ticket.trim());
		if (found == null || found.expiresAt.isBefore(Instant.now())) {
			return null;
		}
		return found.userNo;
	}

	private void evictExpired() {
		Instant now = Instant.now();
		Iterator<Map.Entry<String, Ticket>> iterator = tickets.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().expiresAt.isBefore(now)) {
				iterator.remove();
			}
		}
	}

	private record Ticket(Long userNo, Instant expiresAt) {
	}
}
