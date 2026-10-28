package me._hanho.ultary.common.validation;

/** 닉네임태그 본문: 영문 소문자와 밑줄 1~10자. @는 표시용 접두사. */
public final class NicknameRules {
    public static final String CHAR_REGEX = "^[a-z_]{1,10}$";
    public static final String MESSAGE = "닉네임태그는 영문 소문자와 밑줄(_)만 사용해 1~10자로 입력해주세요.";
    private NicknameRules() {}
    public static boolean isValid(String nickname) {
        return nickname != null && nickname.matches(CHAR_REGEX);
    }
}
