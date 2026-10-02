package me._hanho.ultary.domain.dm.model;

import lombok.Getter;
import lombok.Setter;

/** 메시지 INSERT */
@Getter
@Setter
public class DmMessage {

	private Long dmMessageId;
	private Long dmRoomId;
	private Long senderUserNo;
	private String body;
	/** NONE | FEED | STORY */
	private String shareType;
	private Long feedId;
	private Long feedMediaId;
	private Long storyId;
}
