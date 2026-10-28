package me._hanho.ultary.common.validation;

/** 영문 1자, 완성형 한글 2자로 계산한 펫 이름 제한. */
public final class PetNameRules {
    public static final String MESSAGE = "펫 이름은 영문 대·소문자와 한글만 사용할 수 있으며, 영문 1자·한글 2자로 계산해 30자 이하여야 합니다.";

    private PetNameRules() {}

    public static boolean isValid(String value) {
        if (value == null) return false;
        String name = value.trim();
        if (!name.matches("[A-Za-z가-힣]+")) return false;
        int length = 0;
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            length += character >= '가' && character <= '힣' ? 2 : 1;
        }
        return length <= 30;
    }
}
