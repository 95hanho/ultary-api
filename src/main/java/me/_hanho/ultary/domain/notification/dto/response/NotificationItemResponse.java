package me._hanho.ultary.domain.notification.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class NotificationItemResponse {

	private Long notificationId;
	private String type;
	private String message;
	private Long actorUserNo;
	private String actorNickname;
	private Long actorProfileFileId;
	private FileSummaryResponse actorProfileFile;
	/** 받는 사람 제외 인원. 2 이상이면 message 에 «외 N명» */
	private int actorCount;
	private boolean hasComment;
	private boolean hasReply;
	/** 인용 텍스트 일부. 없으면 null */
	private String snippet;
	private Long feedId;
	private Long feedCommentId;
	private Long feedReplyId;
	private Long storyId;
	private Long neighborId;
	/** PENDING 일 때만 수락 버튼. 이웃 신청이 아니면 null */
	private String neighborStatus;
	private boolean read;
	private LocalDateTime updatedAt;
}
