package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user */
@Getter
@Setter
public class User {

	private Long userNo;
	private String loginId;
	private String password;
	private String name;
	private String nickname;
	private String email;
	private String phone;
	private Integer profileFileId;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String withdrawalStatus;
	private LocalDateTime withdrawalRequestedAt;
	private LocalDateTime withdrawalCompletedAt;
}
