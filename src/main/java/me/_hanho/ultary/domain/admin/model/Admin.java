package me._hanho.ultary.domain.admin.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_admin */
@Getter
@Setter
public class Admin {

	private Long adminNo;
	private String loginId;
	private String password;
	private String name;
	/** SUPER | OPERATOR */
	private String role;
	/** ACTIVE | SUSPENDED */
	private String status;
	private LocalDateTime lastLoginAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
