package me._hanho.ultary.domain.auth.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

	private Long userNo;
	private String name;
	private String nickname;
	/** true면 FE에서 닉네임 변경 유도 */
	private boolean defaultNickname;
	/** 비밀번호가 설정된 계정인지 */
	private boolean hasPassword;
	private String email;
	private String phone;
	private Integer profileFileId;
	private String bio;
	private String regionSido;
	private String regionSigungu;
	private String withdrawalStatus;
	private LocalDateTime createdAt;
}
