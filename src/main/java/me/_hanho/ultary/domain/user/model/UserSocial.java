package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user_social */
@Getter
@Setter
public class UserSocial {

	private Long userSocialId;
	private Long userNo;
	/** GOOGLE | KAKAO */
	private String provider;
	private String providerUserId;
	private String providerEmail;
	private LocalDateTime linkedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
