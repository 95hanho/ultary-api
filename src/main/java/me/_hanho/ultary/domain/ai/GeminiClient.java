package me._hanho.ultary.domain.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** A deadline covers headers AND response body. Credentials never appear in URLs. */
@Component
public class GeminiClient {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public GeminiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public boolean isEnabled() { return properties.isEnabled(); }
    public String modelName() { return StringUtils.hasText(properties.getModel()) ? properties.getModel().trim() : "gemini-2.0-flash"; }

    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isEnabled()) throw new GeminiException("GEMINI_DISABLED", null, "API key is not configured", false);
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json",
                        "thinkingConfig", Map.of("thinkingLevel", properties.getThinkingLevel())));
        long timeout = Math.max(1, properties.getReadTimeoutMs());
        java.util.concurrent.CompletableFuture<HttpResponse<String>> pending = null;
        try {
            String base = properties.getBaseUrl().replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/models/" + modelName() + ":generateContent"))
                    .timeout(Duration.ofMillis(timeout))
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .header("x-goog-api-key", properties.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = pending.get(timeout, TimeUnit.MILLISECONDS);
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new GeminiException("GEMINI_HTTP_" + status, status, "Provider returned HTTP " + status,
                        status == 429 || status >= 500);
            }
            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) throw new GeminiException("GEMINI_EMPTY_CANDIDATE", status, "No generated JSON text", true);
            return text.trim();
        } catch (GeminiException ex) {
            throw ex;
        } catch (TimeoutException ex) {
            throw new GeminiException("GEMINI_TIMEOUT", null, "Response deadline exceeded (" + timeout + "ms)", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiException("GEMINI_INTERRUPTED", null, "Request interrupted", true);
        } catch (ExecutionException ex) {
            Throwable root = ex.getCause();
            String code = root instanceof HttpTimeoutException ? "GEMINI_TIMEOUT" : "GEMINI_NETWORK_FAILED";
            throw new GeminiException(code, null, root == null ? "HTTP transport failed" : root.getClass().getSimpleName(), true);
        } catch (Exception ex) {
            throw new GeminiException("GEMINI_PARSE_OR_CONFIG_FAILED", null, ex.getClass().getSimpleName(), true);
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
    }

    public static class GeminiException extends RuntimeException {
        private final Integer httpStatus;
        private final String diagnostic;
        private final boolean retryable;
        public GeminiException(String code) { this(code, null, code, true); }
        public GeminiException(String code, Throwable cause) { this(code, null, cause.getClass().getSimpleName(), true); }
        public GeminiException(String code, Integer status, String diagnostic, boolean retryable) {
            super(code); this.httpStatus = status; this.diagnostic = diagnostic; this.retryable = retryable;
        }
        public String code() { return getMessage(); }
        public Integer httpStatus() { return httpStatus; }
        public String diagnostic() { return diagnostic; }
        public boolean retryable() { return retryable; }
    }
}
