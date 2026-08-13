package me._hanho.ultary.domain.neighbor.model;

import lombok.Getter;
import lombok.Setter;

/** 주민/이웃 목록 조회용 조인 행 */
@Getter
@Setter
public class NeighborListRow {

	private Long neighborId;
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private String status;
	private String listType;
}
