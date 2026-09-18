package me._hanho.ultary.common.validation;

/**
 * 휴대폰 번호: DB·API는 digits only {@code ^01[0-9]{8,9}$}.
 * 입력은 하이픈/공백/+82 등을 허용하고 {@link #normalize(String)} 후 저장·조회.
 */
public final class PhoneRules {

	public static final String REGEX = "^01[0-9]{8,9}$";
	public static final String MESSAGE = "휴대폰 번호 형식이 올바르지 않습니다. (예: 01012345678)";

	private PhoneRules() {
	}

	/**
	 * trim → 비숫자 제거 → {@code +82}/{@code 82} 국내번호을 {@code 0}으로 치환.
	 * 예: {@code 010-1111-2222}, {@code +82 10-1111-2222} → {@code 01011112222}
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim();
		if (value.isEmpty()) {
			return "";
		}
		if (value.startsWith("+")) {
			value = value.substring(1);
		}
		StringBuilder digits = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c >= '0' && c <= '9') {
				digits.append(c);
			}
		}
		String d = digits.toString();
		if (d.startsWith("82") && d.length() >= 11 && d.charAt(2) == '1') {
			d = "0" + d.substring(2);
		}
		return d;
	}

	public static boolean isValid(String phone) {
		return phone != null && phone.matches(REGEX);
	}

	/** 로그용: {@code 010****2222} */
	public static String maskForLog(String phone) {
		if (phone == null || phone.length() < 7) {
			return "?";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
	}
}
