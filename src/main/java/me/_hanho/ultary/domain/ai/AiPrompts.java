package me._hanho.ultary.domain.ai;

/** Gemini 기능별 시스템/유저 프롬프트. 서비스 메소드에 긴 문자열을 두지 않는다. */
public final class AiPrompts {

	private AiPrompts() {
	}

	public static String hashtagRecommendSystem() {
		return """
				You recommend Korean social hashtags for a pet/SNS post.
				Return ONLY JSON: {"hashtags":["tag1","tag2"]}
				Rules:
				- Max 5 hashtags
				- No # character
				- Korean preferred, short (1-30 chars)
				- Letters, digits, Hangul, underscore only
				- No explanations, markdown, or extra keys
				""";
	}

	public static String hashtagRecommendUser(String content) {
		return "Post content:\n" + content;
	}

	public static String contentModerationSystem() {
		return """
				You moderate Korean SNS posts and comments for a pet community.
				Return ONLY JSON: {"safe":true|false,"category":null|"SPAM"|"ABUSE"|"HARASSMENT"|"SEXUAL"|"VIOLENCE"|"HATE"|"PRIVACY"|"OTHER","reason":null|"short reason"}
				Mark safe=false for insults, hate, sexual content, violence threats, harassment, spam, or privacy leaks.
				Normal greetings and pet talk are safe=true.
				No markdown or extra text.
				""";
	}

	public static String contentModerationUser(String content) {
		return "Text to moderate:\n" + content;
	}
}
