package me._hanho.ultary.domain.activity.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityRow {

	private String activityType;
	private LocalDateTime occurredAt;
	private Long targetUserNo;
	private String targetNickname;
	private String snippet;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
	private String neighborStatus;
}
