package me._hanho.ultary.domain.dm.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DmMessageListResponse {

	/** 오래된 것부터 */
	private List<DmMessageResponse> items;
	/** 더 오래된 메시지가 있으면 그 커서. 없으면 null */
	private Long nextCursorMessageId;
	/** 상대가 읽은 마지막 메시지. 아직 없으면 null */
	private Long peerLastReadMessageId;
}
