package me._hanho.ultary.domain.myultary.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class MyUltaryProfileResponse {

	private Long userNo;
	private String nickname;
	private boolean defaultNickname;
	private Integer profileFileId;
	private FileSummaryResponse profileFile;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	/** 활성 스토리 유무 */
	private boolean hasStory;
	/** 활성 스토리 중 내가 안 읽은 것이 있으면 true. 스토리가 없으면 false */
	private boolean hasUnviewed;
	/** 내가 팔로우한 수. 맞팔 포함 */
	private int followCount;
	/** 나를 팔로우한 수. 맞팔 포함 */
	private int followerCount;
	private int petCount;
	private int feedCount;
}
