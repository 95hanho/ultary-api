package me._hanho.ultary.domain.dm.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DmTypingRequest {

	/** 입력 중이면 true, 멈추면 false */
	private Boolean typing;
}
