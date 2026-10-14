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
	private Long requesterUserNo;
	private String requesterNickname;
	private String title;
	private String content;
	private String link;
	private String status;
	private String rejectMessage;
	private Long reviewedByAdminId;
	private LocalDateTime reviewedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
