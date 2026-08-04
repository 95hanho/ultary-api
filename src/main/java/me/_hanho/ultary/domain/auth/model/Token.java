package me._hanho.ultary.domain.auth.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Token {

	private Long tokenId;
	private String ownerType;
	private Long userNo;
	private Long adminNo;
	private String connectIp;
	private String connectAgent;
	private String refreshToken;
	private LocalDateTime expiresAt;
	private Boolean isRevoked;
	private LocalDateTime revokedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
