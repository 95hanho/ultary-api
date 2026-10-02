package me._hanho.ultary.domain.dm.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_dm_room */
@Getter
@Setter
public class DmRoom {

	private Long dmRoomId;
	private String pairKey;
	private Long userLow;
	private Long userHigh;
	private Long lowLastReadMessageId;
	private Long highLastReadMessageId;
	private LocalDateTime lowLeftAt;
	private LocalDateTime highLeftAt;
	private LocalDateTime createdAt;
}
