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
	/** 최초 조회의 읽음 처리 전 첫 안읽은 상대 메시지. 이전 페이지/안읽음 없음이면 null. */
	private Long firstUnreadMessageId;
	/** 내가 그 방 상대를 팔로우 중. ACCEPTED만 */
	private boolean following;
	/** 그 방 상대가 나를 팔로우 중. ACCEPTED만 */
	private boolean followedBy;
}
