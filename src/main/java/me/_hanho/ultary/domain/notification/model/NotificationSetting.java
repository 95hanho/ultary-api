package me._hanho.ultary.domain.notification.model;

import lombok.Getter;
import lombok.Setter;

/** ultary_notification_setting. null 행은 전부 켜짐 */
@Getter
@Setter
public class NotificationSetting {

	private Long userNo;
	private Boolean neighbor;
	private Boolean likePost;
	private Boolean likeComment;
	private Boolean likeReply;
	private Boolean commentOnPost;
	private Boolean replyOnComment;
	private Boolean mention;
	private Boolean tagPost;
	private Boolean tagStory;
	private Boolean storyReact;
}
