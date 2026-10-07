package me._hanho.ultary.domain.dm;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.ws.RealtimePush;

/**
 * 채팅방을 보고 있는지, 입력 중인지는 이 서버 메모리에만 둔다.
 * 보고 있음은 20초, 입력 중은 4초가 지나면 스스로 사라진다.
 */
@Component
@RequiredArgsConstructor
public class DmPresence {

	private static final long VIEWING_TTL_MILLIS = 20_000;
	private static final long TYPING_TTL_MILLIS = 4_000;

	private final RealtimePush realtimePush;
	private final ConcurrentHashMap<Long, View> viewing = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, Typing> typing = new ConcurrentHashMap<>();

	public void view(Long userNo, Long roomId) {
		viewing.put(userNo, new View(roomId, System.currentTimeMillis() + VIEWING_TTL_MILLIS));
	}

	public void leave(Long userNo, Long roomId) {
		viewing.computeIfPresent(userNo, (key, view) -> view.roomId.equals(roomId) ? null : view);
		stopTyping(userNo, roomId);
	}

	public boolean isViewing(Long userNo, Long roomId) {
		View view = viewing.get(userNo);
		if (view == null || !view.roomId.equals(roomId)) {
			return false;
		}
		if (view.expiresAt < System.currentTimeMillis()) {
			viewing.remove(userNo, view);
			return false;
		}
		return true;
	}

	public void typing(Long userNo, Long peerUserNo, Long roomId, boolean active) {
		if (!active) {
			stopTyping(userNo, roomId);
			return;
		}
		typing.put(userNo, new Typing(roomId, peerUserNo, System.currentTimeMillis() + TYPING_TTL_MILLIS));
		realtimePush.pushDmTyping(peerUserNo, roomId, userNo, true);
	}

	public void onDisconnect(Long userNo) {
		viewing.remove(userNo);
		Typing current = typing.remove(userNo);
		if (current != null) {
			realtimePush.pushDmTyping(current.peerUserNo, current.roomId, userNo, false);
		}
	}

	@Scheduled(fixedRate = 1_000)
	public void expire() {
		long now = System.currentTimeMillis();
		viewing.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
		typing.entrySet().removeIf(entry -> {
			Typing current = entry.getValue();
			if (current.expiresAt >= now) {
				return false;
			}
			realtimePush.pushDmTyping(current.peerUserNo, current.roomId, entry.getKey(), false);
			return true;
		});
	}

	private void stopTyping(Long userNo, Long roomId) {
		Typing current = typing.get(userNo);
		if (current == null || !current.roomId.equals(roomId)) {
			return;
		}
		if (typing.remove(userNo, current)) {
			realtimePush.pushDmTyping(current.peerUserNo, current.roomId, userNo, false);
		}
	}

	private record View(Long roomId, long expiresAt) {
	}

	private record Typing(Long roomId, Long peerUserNo, long expiresAt) {
	}
}
