package me._hanho.ultary.domain.neighbor.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class BlockedUserItemResponse {

	/** 해제 API 경로의 userNo */
	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	/** 대표 펫 사진. 없으면 null */
	private FileSummaryResponse profileFile;
	/** 차단한 시각. 다시 차단하면 그 시각 */
	private LocalDateTime blockedAt;
}
