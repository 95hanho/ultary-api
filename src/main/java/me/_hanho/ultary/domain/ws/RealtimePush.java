package me._hanho.ultary.domain.ws;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.dm.dto.response.DmMessageResponse;
import me._hanho.ultary.domain.dm.dto.response.DmRoomItemResponse;
import me._hanho.ultary.domain.notification.NotificationMapper;

@Service
@RequiredArgsConstructor
public class RealtimePush {

	private final WsSessionRegistry registry;
	private final NotificationMapper notificationMapper;

	public void pushNotificationUnread(Long userNo) {
		if (userNo == null) {
			return;
		}
		afterCommit(() -> registry.send(userNo, Map.of(
				"type", "NOTIFICATION_UNREAD",
				"unreadCount", notificationMapper.countUnread(userNo))));
	}

	public void pushDm(Long userNo, DmRoomItemResponse room, DmMessageResponse message) {
		if (userNo == null || room == null || message == null) {
			return;
		}
		afterCommit(() -> registry.send(userNo, Map.of(
				"type", "DM_MESSAGE",
				"dmRoomId", room.getDmRoomId(),
				"room", room,
				"message", message)));
	}

	/** 열린 트랜잭션이 커밋된 뒤에 보낸다. 없으면 바로 보낸다 */
	public void afterCommit(Runnable task) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					task.run();
				}
			});
			return;
		}
		task.run();
	}
}
