package me._hanho.ultary.domain.dm.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** 메시지 + 공유 대상 조인 */
@Getter
@Setter
public class DmMessageRow {

	private Long dmMessageId;
	private Long senderUserNo;
	private String body;
	private String shareType;
	private Long feedId;
	private Long feedMediaId;
	private Long storyId;
	private LocalDateTime createdAt;

	private String feedContent;
	private Integer feedDeleted;
	private Long authorUserNo;
	private String authorNickname;
	private Long authorProfileFileId;
	private Integer mediaIndex;
	private Long mediaFileId;
	private Long mediaThumbnailFileId;
	private String mediaType;

	private Integer storyDeleted;
	private Long storyFileId;
	private Long storyThumbnailFileId;
	private String storyMediaType;
}
