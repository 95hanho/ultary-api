package me._hanho.ultary.domain.main.dto.response;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class MainSearchPetItem {

	private Long petId;
	private Long userNo;
	private String mentionId;
	private String name;
	private String species;
	private Long profileFileId;
	private FileSummaryResponse profileFile;
}
