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
	/** 내가 팔로우(요청·수락)한 수 = 주민 */
	private int residentCount;
	/** 나를 팔로우(요청·수락)한 수 = 이웃 */
	private int neighborCount;
	private int petCount;
	private int feedCount;
}
