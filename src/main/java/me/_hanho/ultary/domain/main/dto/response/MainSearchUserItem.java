package me._hanho.ultary.domain.main.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MainSearchUserItem {

	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private String bio;
}
