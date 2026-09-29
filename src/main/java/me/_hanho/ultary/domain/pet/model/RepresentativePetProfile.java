package me._hanho.ultary.domain.pet.model;

import lombok.Getter;
import lombok.Setter;

/** 유저별 대표 프로필 사진. 사진 있는 펫 중 priority가 가장 높은 행 */
@Getter
@Setter
public class RepresentativePetProfile {

	private Long userNo;
	private Integer profileFileId;
}
