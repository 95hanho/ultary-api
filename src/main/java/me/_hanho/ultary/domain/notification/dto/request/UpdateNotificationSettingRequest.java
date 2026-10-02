package me._hanho.ultary.domain.notification.dto.request;

import lombok.Getter;
import lombok.Setter;

/** 넣은 항목만 바꾼다. 빠지면 기존 값 */
@Getter
@Setter
public class UpdateNotificationSettingRequest {

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

	public boolean hasAny() {
		return neighbor != null
				|| likePost != null
				|| likeComment != null
				|| likeReply != null
				|| commentOnPost != null
				|| replyOnComment != null
				|| mention != null
				|| tagPost != null
				|| tagStory != null
				|| storyReact != null;
	}
}
