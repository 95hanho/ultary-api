package me._hanho.ultary.domain.feed.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentMentionResponse {

	/** USER | PET */
	private String type;
	private Long userNo;
	private String nickname;
	private Long petId;
	private String mentionId;
	private String petName;
}
