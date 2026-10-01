package me._hanho.ultary.common.validation;

/**
 * 유저 닉네임: 영문 대소문자, 숫자, 한글.
 * <ul>
 *   <li>한글만: 2~5자</li>
 *   <li>영문·숫자만: 4~10자 (각 1자)</li>
 *   <li>혼합: 한글 1자 = 가중치 2, 영문·숫자 1자 = 1. 가중치 합 4~10, 한글은 최대 5자</li>
 * </ul>
 */
public final class NicknameRules {

	/** 허용 문자만. 길이·가중치는 {@link #isValid(String)} */
	public static final String CHAR_REGEX = "^[A-Za-z0-9가-힣]+$";

	public static final String MESSAGE =
			"닉네임은 한글 2~5자 또는 영문·숫자 4~10자입니다. 섞어 쓸 때는 한글 1자를 2자로, 영문·숫자는 1자로 계산해 합 10자를 넘을 수 없습니다.";

	private static final int HANGUL_MIN = 2;
	private static final int HANGUL_MAX = 5;
	private static final int SINGLE_MIN = 4;
	private static final int SINGLE_MAX = 10;
	private static final int WEIGHTED_MIN = 4;
	private static final int WEIGHTED_MAX = 10;

	private NicknameRules() {
	}

	public static boolean isValid(String nickname) {
		if (nickname == null || nickname.isEmpty()) {
			return false;
		}
		if (!nickname.matches(CHAR_REGEX)) {
			return false;
		}

		int hangul = 0;
		int single = 0;
		for (int i = 0; i < nickname.length(); i++) {
			char c = nickname.charAt(i);
			if (c >= '가' && c <= '힣') {
				hangul++;
			} else {
				single++;
			}
		}

		if (hangul > HANGUL_MAX || single > SINGLE_MAX) {
			return false;
		}

		int weighted = hangul * 2 + single;
		if (weighted > WEIGHTED_MAX) {
			return false;
		}

		if (single == 0) {
			return hangul >= HANGUL_MIN && hangul <= HANGUL_MAX;
		}
		if (hangul == 0) {
			return single >= SINGLE_MIN && single <= SINGLE_MAX;
		}
		return weighted >= WEIGHTED_MIN;
	}
}
