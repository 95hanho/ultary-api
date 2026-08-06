package me._hanho.ultary.domain.neighbor.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_neighbor */
@Getter
@Setter
public class Neighbor {

	private Long neighborId;
	private Long requesterUserNo;
	private Long receiverUserNo;
	private String pairKey;
	/** PENDING | ACCEPTED | REJECTED | BLOCKED */
	private String status;
	private LocalDateTime requestedAt;
	private LocalDateTime acceptedAt;
	private LocalDateTime rejectedAt;
	private LocalDateTime blockedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
