package me._hanho.ultary.domain.notification.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationListRow {

	private Long notificationId;
	private String type;
	private Long actorUserNo;
	private String actorNickname;
	private Integer actorCount;
	private Boolean hasComment;
	private Boolean hasReply;
	private String content;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
	private String neighborStatus;
	private Boolean isRead;
	private LocalDateTime updatedAt;
}
