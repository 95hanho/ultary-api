package me._hanho.ultary.domain.main.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class PetTagHistoryItemResponse {

	private Long petId;
	private String mentionId;
	private String name;
	private Long userNo;
	private String ownerNickname;
	private Long profileFileId;
	private FileSummaryResponse profileFile;
	private LocalDateTime usedAt;
}
