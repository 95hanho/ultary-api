package me._hanho.ultary.domain.neighbor.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUltaryResponse {

	private Long userNo;
	private String nickname;
	private boolean defaultNickname;
	private Integer profileFileId;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	private boolean hasStory;
	private int residentCount;
	private int neighborCount;
	private int petCount;
	private int feedCount;
	/**
	 * 나와의 관계:
	 * NONE | PENDING_SENT | PENDING_RECEIVED | ACCEPTED | REJECTED | BLOCKED
	 */
	private String relationStatus;
	private Long neighborId;
	private boolean blockedByMe;
	private boolean blockedMe;
}
