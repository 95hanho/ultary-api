package me._hanho.ultary.domain.notification.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_notification */
@Getter
@Setter
public class Notification {

	private Long notificationId;
	private Long receiverUserNo;
	private Long actorUserNo;
	/**
	 * FEED_LIKE | FEED_COMMENT | FEED_REPLY | MENTION |
	 * NEIGHBOR_REQUEST | NEIGHBOR_ACCEPTED | PET_TAG_REQUEST | PET_TAG_APPROVED | SYSTEM
	 */
	private String type;
	private Long feedId;
	private Long feedPetId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long neighborId;
	private String content;
	private Boolean isRead;
	private LocalDateTime readAt;
	private LocalDateTime createdAt;
}
