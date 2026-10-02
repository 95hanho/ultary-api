package me._hanho.ultary.domain.dm.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class DmRoomItemResponse {

	private Long dmRoomId;
	private Long peerUserNo;
	private String peerNickname;
	private FileSummaryResponse profileFile;
	/** 마지막 글. 공유만 있으면 짧은 안내. 메시지가 없으면 null */
	private String lastMessage;
	private LocalDateTime lastMessageAt;
	private int unreadCount;
}
