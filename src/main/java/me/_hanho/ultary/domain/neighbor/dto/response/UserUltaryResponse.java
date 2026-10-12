package me._hanho.ultary.domain.neighbor.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.report.dto.response.MyReportResponse;

@Getter
@Builder
public class UserUltaryResponse {

	private Long userNo;
	private String nickname;
	private boolean defaultNickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	private boolean hasStory;
	/** 그 울타리 활성 스토리 중 내가 안 읽은 것이 있으면 true. 스토리가 없으면 false */
	private boolean hasUnviewed;
	/** 이 유저가 팔로우한 수 */
	private int followCount;
	/** 이 유저를 팔로우한 수 */
	private int followerCount;
	private int petCount;
	private int feedCount;
	/**
	 * 나와의 관계:
	 * NONE | PENDING_SENT | PENDING_RECEIVED | ACCEPTED | REJECTED | BLOCKED
	 */
	private String relationStatus;
	/** 비공개 계정. 본인이 아니고 following이 false면 게시글·고정·태그 그리드는 빈 배열 */
	private boolean privateAccount;
	/** 내가 이 유저를 팔로우 중 */
	private boolean following;
	/** 이 유저가 나를 팔로우 중 (팔로워) */
	private boolean followedBy;
	private Long neighborId;
	private boolean blockedByMe;
	private boolean blockedMe;
	/** 내 신고. 없으면 null. status가 REQUESTED면 취소 가능 */
	private MyReportResponse myReport;
}
