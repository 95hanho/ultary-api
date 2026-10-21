package me._hanho.ultary.domain.auth.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_token */
@Getter
@Setter
public class Token {

	private Long tokenId;
	private String ownerType;
	private Long userNo;
	private Long adminNo;
	private String sessionId;
	private LocalDateTime loggedInAt;
	private String connectIp;
	private String connectAgent;
	private String refreshToken;
	private LocalDateTime expiresAt;
	private Boolean isRevoked;
	private LocalDateTime revokedAt;
	private Long replacedByTokenId;
	private LocalDateTime rotatedAt;
	/** DB 시각 기준으로 계산한 중복 재발급 허용 여부. */
	private Boolean rotationRetryAllowed;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
