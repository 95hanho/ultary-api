package me._hanho.ultary.domain.notification.model;

import lombok.Getter;
import lombok.Setter;

/** 팔로우 알림을 맞출 때 쓰는 관계 한 줄 */
@Getter
@Setter
public class NeighborNotice {

	private Long neighborId;
	private Long requesterUserNo;
	private Long receiverUserNo;
	private String status;
	private Boolean followedBack;
}
