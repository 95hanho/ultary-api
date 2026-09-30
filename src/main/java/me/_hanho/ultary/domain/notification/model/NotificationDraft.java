package me._hanho.ultary.domain.notification.model;

import lombok.Getter;
import lombok.Setter;

/** 집계 조회 결과. actor 가 없으면 알림 행을 지운다. */
@Getter
@Setter
public class NotificationDraft {

	private Long receiverUserNo;
	private Long actorUserNo;
	private Integer actorCount;
	private String content;
	private Integer hasComment;
	private Integer hasReply;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
}
