package me._hanho.ultary.domain.feed.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedReplyResponse {

	private Long feedReplyId;
	private Long feedCommentId;
	private Long userNo;
	private String authorNickname;
	private String content;
	private List<CommentMentionResponse> mentions;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
