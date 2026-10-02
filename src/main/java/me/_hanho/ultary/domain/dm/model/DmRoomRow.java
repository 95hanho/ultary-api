package me._hanho.ultary.domain.dm.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 대화방 목록 한 줄 */
@Getter
@Setter
public class DmRoomRow {

	private Long dmRoomId;
	private Long peerUserNo;
	private String peerNickname;
	private Long profileFileId;
	private String lastBody;
	private String lastShareType;
	private LocalDateTime lastMessageAt;
	private Long unreadCount;
}
