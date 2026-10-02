package me._hanho.ultary.common.validation;

/** pet.mention_id / tag.handle: 영문·숫자·언더바 */
public final class HandleRules {

	public static final String REGEX = "^[A-Za-z0-9_]{1,30}$";
	public static final String MESSAGE = "핸들은 영문, 숫자, 언더바(_)만 사용할 수 있습니다. (1~30자)";
	/** 펫 멘션 ID(펫 태그) 검증 실패 문구. 태그 핸들 MESSAGE와 규칙을 공유한다. */
	public static final String MENTION_MESSAGE = "펫 태그는 영문, 숫자, 언더바(_)만 사용할 수 있습니다. (1~30자)";

	private HandleRules() {
	}
}
