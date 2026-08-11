package me._hanho.ultary.common.validation;

/**
 * 비밀번호 규칙 (회원가입 / 비밀번호 변경 / 로컬 테스트 인코딩 API 공통).
 */
public final class PasswordRules {

	private PasswordRules() {
	}

	/**
	 * 8자 이상 100자 이하,
	 * 영문(대·소문자 구분 없이 1자 이상) + 숫자 1자 이상 + 특수문자 1자 이상.
	 * 첫 글자 종류 제한 없음.
	 *
	 * <pre>
	 * ^                         문자열 시작
	 * (?=.*[A-Za-z])            문자열 어디든 영문 대문자 또는 소문자 1자 이상
	 * (?=.*\d)                  문자열 어디든 숫자 1자 이상
	 * (?=.*[!@#$%^&*()_+\-={}\[\]|;:'",.&lt;&gt;/?~`\\])  특수문자 1자 이상
	 * .{8,100}                  전체 길이 8~100자
	 * $                         문자열 끝
	 * </pre>
	 */
	public static final String REGEX =
			"^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|;:'\",.<>/?~`\\\\]).{8,100}$";

	public static final String MESSAGE =
			"비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.";
}
