package me._hanho.ultary.domain.neighbor.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockedUserRow {

	private Long userNo;
	private String nickname;
	private Integer profileFileId;
	private LocalDateTime blockedAt;
}
