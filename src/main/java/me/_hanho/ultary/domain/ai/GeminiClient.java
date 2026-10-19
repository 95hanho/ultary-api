package me._hanho.ultary.domain.ai;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Gemini generateContent HTTP 호출. API Key는 로그·예외 메시지에 넣지 않는다.
 */
@Slf4j
@Component
public class GeminiClient {

	private final AiProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public GeminiClient(AiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Math.max(1, properties.getConnectTimeoutMs()));
		factory.setReadTimeout(Math.max(1, properties.getReadTimeoutMs()));
		this.restClient = RestClient.builder()
				.baseUrl(trimTrailingSlash(properties.getBaseUrl()))
				.requestFactory(factory)
				.build();
	}

	public boolean isEnabled() {
		return properties.isEnabled();
	}

	public String modelName() {
		return StringUtils.hasText(properties.getModel()) ? properties.getModel().trim() : "gemini-2.0-flash";
	}

	/**
	 * system + user 텍스트로 JSON 본문 문자열을 받는다.
	 * @throws GeminiException 네트워크·HTTP·빈 응답
	 */
	public String generateJson(String systemPrompt, String userPrompt) {
		if (!isEnabled()) {
			throw new GeminiException("GEMINI_DISABLED");
		}
		String model = modelName();
		String path = "/models/" + model + ":generateContent";
		Map<String, Object> body = Map.of(
				"systemInstruction", Map.of(
						"parts", List.of(Map.of("text", systemPrompt))),
				"contents", List.of(Map.of(
						"role", "user",
						"parts", List.of(Map.of("text", userPrompt)))),
				"generationConfig", Map.of(
						"temperature", 0.2,
						"responseMimeType", "application/json"));

		try {
			String raw = restClient.post()
					.uri(uriBuilder -> uriBuilder
							.path(path)
							.queryParam("key", properties.getApiKey().trim())
							.build())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(String.class);
			return extractText(raw);
		} catch (RestClientException ex) {
			log.warn("[GeminiClient] generateContent failed: {}", safeMessage(ex));
			throw new GeminiException("GEMINI_HTTP_FAILED", ex);
		}
	}

	private String extractText(String raw) {
		if (!StringUtils.hasText(raw)) {
			throw new GeminiException("GEMINI_EMPTY_RESPONSE");
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
			if (text.isMissingNode() || !StringUtils.hasText(text.asText())) {
				throw new GeminiException("GEMINI_EMPTY_CANDIDATE");
			}
			return text.asText().trim();
		} catch (GeminiException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new GeminiException("GEMINI_PARSE_FAILED", ex);
		}
	}

	private static String trimTrailingSlash(String url) {
		if (!StringUtils.hasText(url)) {
			return "https://generativelanguage.googleapis.com/v1beta";
		}
		String value = url.trim();
		while (value.endsWith("/")) {
			value = value.substring(0, value.length() - 1);
		}
		return value;
	}

	private static String safeMessage(Throwable ex) {
		String message = ex.getMessage();
		if (message == null) {
			return ex.getClass().getSimpleName();
		}
		return message.replaceAll("(?i)key=[^&\\s]+", "key=***");
	}

	public static class GeminiException extends RuntimeException {
		public GeminiException(String code) {
			super(code);
		}

		public GeminiException(String code, Throwable cause) {
			super(code, cause);
		}

		public String code() {
			return getMessage();
		}
	}
}
