package me._hanho.ultary.domain.neighbor.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NeighborRelationResponse {

	private Long neighborId;
	private Long requesterUserNo;
	private Long receiverUserNo;
	/** PENDING | ACCEPTED | REJECTED | BLOCKED */
	private String status;
	/** 수신자도 요청자를 팔로우 중이면 true */
	private boolean followedBack;
}
