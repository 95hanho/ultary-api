package me._hanho.ultary.domain.notification.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationListResponse {

	private int unreadCount;
	private List<NotificationItemResponse> items;
}
