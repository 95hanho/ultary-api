package me._hanho.ultary.domain.activity.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class ActivityItemResponse {

	/** FEED_LIKE, COMMENT_LIKE, REPLY_LIKE, FEED_COMMENT, FEED_REPLY, NEIGHBOR_REQUEST, FEED, STORY */
	private String type;
	private LocalDateTime occurredAt;
	private Long targetUserNo;
	private String targetNickname;
	/** 대상 유저의 대표 펫 사진. 없으면 null */
	private Long profileFileId;
	private FileSummaryResponse profileFile;
	/** 화면 미리보기 원문. 없으면 null */
	private String snippet;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
	/** 내가 보낸 이웃 신청 상태. PENDING 일 때만 취소. 이웃이 아니면 null */
	private String neighborStatus;
}
