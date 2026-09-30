package me._hanho.ultary.domain.notification.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationUpsert {

	private Long receiverUserNo;
	private Long actorUserNo;
	private String type;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
	private String content;
	private int actorCount;
	private int hasComment;
	private int hasReply;
	private String groupKey;
	/** 1이면 안 읽음으로 되돌리고 updated_at 을 갱신 */
	private int markUnread;
}
