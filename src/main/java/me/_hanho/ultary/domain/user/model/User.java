package me._hanho.ultary.domain.user.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_user */
@Getter
@Setter
public class User {

	private Long userNo;
	private String password;
	/** 회원 비밀번호 마지막 변경. 가입·관리자 초기화 시 null */
	private LocalDateTime passwordChangedAt;
	/** 관리자 초기화 후 비밀번호 변경 필요 */
	private Boolean mustChangePassword;
	private String name;
	private String nickname;
	private LocalDateTime nicknameChangedAt;
	/** 직전 닉네임태그. 변경 시 설정. 7일간 선점·리다이렉트에만 사용 */
	private String previousNickname;
	/** 1=소셜 자동 닉네임태그(변경 유도), 0=사용자 변경 완료 */
	private Boolean isDefaultNickname;
	private String email;
	private String phone;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private String withdrawalStatus;
	private LocalDateTime withdrawalCompletedAt;
	private LocalDateTime suspendedAt;
}
