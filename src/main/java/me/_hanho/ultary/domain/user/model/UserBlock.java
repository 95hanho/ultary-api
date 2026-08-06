package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user_block */
@Getter
@Setter
public class UserBlock {

	private Long userBlockId;
	private Long blockerUserNo;
	private Long blockedUserNo;
	private String reason;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
