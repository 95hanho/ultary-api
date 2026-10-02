package me._hanho.ultary.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingResponse {

	private boolean neighbor;
	private boolean likePost;
	private boolean likeComment;
	private boolean likeReply;
	private boolean commentOnPost;
	private boolean replyOnComment;
	private boolean mention;
	private boolean tagPost;
	private boolean tagStory;
	private boolean storyReact;
}
