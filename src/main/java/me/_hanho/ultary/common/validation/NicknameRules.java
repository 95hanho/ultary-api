package me._hanho.ultary.common.validation;

/** 유저 닉네임: 영문 대소문자 + 한글만 */
public final class NicknameRules {

	public static final String REGEX = "^[A-Za-z가-힣]{1,30}$";
	public static final String MESSAGE = "닉네임은 영문 대소문자와 한글만 사용할 수 있습니다. (1~30자)";

	private NicknameRules() {
	}
}
