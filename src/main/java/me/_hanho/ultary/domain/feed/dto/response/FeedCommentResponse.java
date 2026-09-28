package me._hanho.ultary.domain.feed.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class FeedCommentResponse {

	private Long feedCommentId;
	private Long feedId;
	private Long userNo;
	private String authorNickname;
	/** 작성자 프로필. 미등록·삭제 파일이면 null */
	private FileSummaryResponse authorProfileFile;
	private String content;
	private List<CommentMentionResponse> mentions;
	private Integer likeCount;
	private Boolean likedByMe;
	private Integer replyCount;
	/**
	 * 게시글의 댓글+답글 합이 10 이하일 때만 채움.
	 * 초과 시 null → FE는 GET .../replies 로 펼치기.
	 */
	private List<FeedReplyResponse> replies;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
