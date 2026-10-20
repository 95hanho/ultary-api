package me._hanho.ultary.domain.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.gemini")
public class AiProperties {

	/** 환경변수 GEMINI_API_KEY. 비어 있으면 AI 호출 안 함 */
	private String apiKey = "";

	/** 환경변수 GEMINI_MODEL */
	private String model = "gemini-2.0-flash";

	private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

	/** 환경변수 GEMINI_THINKING_LEVEL. 태그 추천·모더레이션 공통 */
	private String thinkingLevel = "low";

	/** 연결 타임아웃(ms) */
	private int connectTimeoutMs = 3000;

	/** 읽기 타임아웃(ms) */
	private int readTimeoutMs = 8000;

	public boolean isEnabled() {
		return apiKey != null && !apiKey.isBlank();
	}
}
