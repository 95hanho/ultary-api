package me._hanho.ultary.domain.neighbor.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class NeighborListItemResponse {

	private Long neighborId;
	/** 목록에 표시되는 상대 유저 */
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	/** PENDING | ACCEPTED | REJECTED | BLOCKED */
	private String status;
	/** FOLLOW(내가 팔로우) | FOLLOWER(상대가 팔로우) */
	private String listType;
	/** 목록 주인과 상대가 서로 팔로우 중이면 true */
	private boolean mutual;
	private boolean hasStory;
	/** 조회자가 볼 수 있는 스토리 중 미열람 스토리가 있으면 true */
	private boolean hasUnviewed;
	/** 그 유저 펫 mentionId. @ 없음. priority 순. 없으면 빈 배열 */
	private List<String> petTags;
	/** 로그인한 조회자가 이 유저를 팔로우 중이면 true */
	private boolean following;
}
