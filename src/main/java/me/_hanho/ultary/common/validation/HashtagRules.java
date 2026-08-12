package me._hanho.ultary.common.validation;

/** tag.hashtag: # 없이 저장. 영문·숫자·한글·언더바 */
public final class HashtagRules {

	public static final String REGEX = "^[A-Za-z0-9가-힣_]{1,30}$";
	public static final String MESSAGE = "해시태그는 영문, 숫자, 한글, 언더바(_)만 사용할 수 있습니다. (1~30자, # 제외)";

	private HashtagRules() {
	}

	/** 앞의 # 제거 후 trim */
	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		String value = raw.trim();
		while (value.startsWith("#")) {
			value = value.substring(1).trim();
		}
		return value;
	}
}
