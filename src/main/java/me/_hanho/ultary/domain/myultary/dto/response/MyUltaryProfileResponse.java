package me._hanho.ultary.domain.myultary.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyUltaryProfileResponse {

	private Long userNo;
	private String nickname;
	private boolean defaultNickname;
	private Integer profileFileId;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	/** 스토리 테이블 미구현 — 항상 false */
	private boolean hasStory;
	/** 내가 팔로우(요청·수락)한 수 = 주민 */
	private int residentCount;
	/** 나를 팔로우(요청·수락)한 수 = 이웃 */
	private int neighborCount;
	private int petCount;
	private int feedCount;
}
