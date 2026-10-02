package me._hanho.ultary.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationUnreadCountResponse {

	/** 하단 배지. 알림 페이지를 열기 전까지 쌓인다 */
	private int unreadCount;
}
