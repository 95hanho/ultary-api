package me._hanho.ultary.domain.dm.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class DmShareTargetItemResponse {

	private Long userNo;
	private String nickname;
	private FileSummaryResponse profileFile;
	/** 이미 대화방이 있으면 그 번호. 없으면 null */
	private Long dmRoomId;
	/** 내가 상대를 팔로우 중 */
	private boolean following;
	/** 상대가 나를 팔로우 중 */
	private boolean followedBy;
	/**
	 * 1 한 달 안 DM, 2 한 달 안 내 팔로우, 3 한 달 안 팔로워, 4 그 외 날짜순.
	 * 한 사람은 더 앞 그룹에만 있다.
	 */
	private int group;
	/** 그 그룹에서 정렬에 쓴 시각 */
	private LocalDateTime activeAt;
}
