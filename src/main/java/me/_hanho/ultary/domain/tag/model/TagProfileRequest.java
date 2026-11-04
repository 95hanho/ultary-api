package me._hanho.ultary.domain.tag.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_tag_profile_request */
@Getter
@Setter
public class TagProfileRequest {

	private Long tagProfileRequestId;
	private Long tagId;
	private String hashtag;
	/** 태그(ultary_tag) 소프트 삭제 여부 */
	private Boolean tagDeleted;
	private Integer feedCount;
	private Long requesterUserNo;
	private String requesterNickname;
	private String title;
	private String content;
	private String link;
	private String status;
	private String rejectMessage;
	private Long reviewedByAdminId;
	private LocalDateTime reviewedAt;
	private LocalDateTime statusChangedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
