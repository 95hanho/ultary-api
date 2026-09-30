package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 최근 펫 태그 목록 한 줄 */
@Getter
@Setter
public class PetTagHistoryRow {

	private Long userPetTagHistoryId;
	private Long petId;
	private String mentionId;
	private String name;
	private Long ownerUserNo;
	private String ownerNickname;
	private Long profileFileId;
	private LocalDateTime usedAt;
}
