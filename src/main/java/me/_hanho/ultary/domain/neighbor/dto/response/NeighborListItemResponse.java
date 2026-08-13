package me._hanho.ultary.domain.neighbor.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NeighborListItemResponse {

	private Long neighborId;
	/** 목록에 표시되는 상대 유저 */
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	/** PENDING | ACCEPTED | REJECTED | BLOCKED */
	private String status;
	/** RESIDENTS(내가 요청) | NEIGHBORS(상대가 요청) */
	private String listType;
	private boolean hasStory;
}
