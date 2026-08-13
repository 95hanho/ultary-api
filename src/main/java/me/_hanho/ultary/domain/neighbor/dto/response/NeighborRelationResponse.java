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
}
