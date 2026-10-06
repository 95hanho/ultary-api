package me._hanho.ultary.domain.ws.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WsTicketResponse {

	private String ticket;
	/** 초 */
	private int expiresIn;
}
