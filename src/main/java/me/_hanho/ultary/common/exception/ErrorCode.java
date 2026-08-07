package me._hanho.ultary.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// ==============================
	// 공통 (common)
	// ==============================
	INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_INPUT("INVALID_INPUT", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
	NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	DB_CONNECTION_FAILED("DB_CONNECTION_FAILED", "데이터베이스 연결에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
	NOT_IMPLEMENTED("NOT_IMPLEMENTED", "아직 구현되지 않은 API입니다.", HttpStatus.NOT_IMPLEMENTED),

	// ==============================
	// 인증 (auth)
	// ==============================
	LOGIN_FAILED("LOGIN_FAILED", "이메일/휴대폰 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
	USER_INACTIVE("USER_INACTIVE", "이용할 수 없는 계정입니다.", HttpStatus.FORBIDDEN),
	TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
	INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
	NICKNAME_DUPLICATED("NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
	PHONE_AUTH_FAILED("PHONE_AUTH_FAILED", "휴대폰 인증에 실패했습니다.", HttpStatus.BAD_REQUEST),
	PHONE_ALREADY_USED("PHONE_ALREADY_USED", "이미 가입된 휴대폰 번호입니다.", HttpStatus.CONFLICT),
	PASSWORD_CHANGE_FAILED("PASSWORD_CHANGE_FAILED", "비밀번호 변경에 실패했습니다.", HttpStatus.BAD_REQUEST),
	INVALID_SOCIAL_PROVIDER("INVALID_SOCIAL_PROVIDER", "지원하지 않는 소셜 제공자입니다.", HttpStatus.BAD_REQUEST),
	SOCIAL_ALREADY_LINKED("SOCIAL_ALREADY_LINKED", "이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT),
	SOCIAL_ACCOUNT_IN_USE("SOCIAL_ACCOUNT_IN_USE", "다른 계정에 이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT),
	SOCIAL_NOT_LINKED("SOCIAL_NOT_LINKED", "연동되지 않은 소셜 계정입니다.", HttpStatus.NOT_FOUND),
	CANNOT_UNLINK_LAST_LOGIN("CANNOT_UNLINK_LAST_LOGIN", "마지막 로그인 수단은 해제할 수 없습니다.", HttpStatus.BAD_REQUEST),

	// ==============================
	// 파일 (file)
	// ==============================
	// FILE_NOT_FOUND("FILE_NOT_FOUND", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 유저 (user)
	// ==============================
	USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 반려동물 (pet)
	// ==============================
	// PET_NOT_FOUND("PET_NOT_FOUND", "반려동물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 피드 (feed)
	// ==============================
	// FEED_NOT_FOUND("FEED_NOT_FOUND", "게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 이웃 (neighbor)
	// ==============================
	// NEIGHBOR_NOT_FOUND("NEIGHBOR_NOT_FOUND", "이웃 관계를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 태그 (tag)
	// ==============================
	// TAG_NOT_FOUND("TAG_NOT_FOUND", "태그를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 알림 (notification)
	// ==============================
	// NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 신고 (report)
	// ==============================
	// REPORT_NOT_FOUND("REPORT_NOT_FOUND", "신고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

	// ==============================
	// 관리자 (admin)
	// ==============================
	// ADMIN_NOT_FOUND("ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	;

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;
}
