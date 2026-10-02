package me._hanho.ultary.domain.dm.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DmMessageResponse {

	private Long dmMessageId;
	private Long senderUserNo;
	private boolean fromMe;
	/** 같이 보낸 글. 없으면 null */
	private String body;
	private LocalDateTime createdAt;
	/** 글만 있으면 null */
	private DmShareResponse share;
}
