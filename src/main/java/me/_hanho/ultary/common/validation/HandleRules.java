package me._hanho.ultary.common.validation;

/** pet.mention_id / tag.handle: 영문·숫자·언더바 */
public final class HandleRules {

	public static final String REGEX = "^[A-Za-z0-9_]{1,30}$";
	public static final String MESSAGE = "핸들은 영문, 숫자, 언더바(_)만 사용할 수 있습니다. (1~30자)";

	private HandleRules() {
	}
}
