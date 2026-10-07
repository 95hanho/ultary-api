package me._hanho.ultary.domain.dm.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DmViewingRequest {

	/** 이 대화방 화면을 보고 있으면 true */
	private Boolean viewing;
}
