package me._hanho.ultary.domain.dm.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 게시글 공유 대상 후보. 한 사람 1행 */
@Getter
@Setter
public class ShareTargetRow {

	private Long userNo;
	private String nickname;
	private Long profileFileId;
	private Long dmRoomId;
	private LocalDateTime lastMessageAt;
	/** 내가 그 사람을 팔로우한 시각. 없으면 null */
	private LocalDateTime followedAt;
	/** 그 사람이 나를 팔로우한 시각. 없으면 null */
	private LocalDateTime followerAt;
}
