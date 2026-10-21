package me._hanho.ultary.security.bff;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

class BffAuthenticationFilterTest {

	final String key = "unit-test-bff-key-not-for-production-123456789";
	BffSecurityProperties properties;
	BffAuthenticationFilter filter;
	MockHttpServletResponse response;
	AtomicBoolean reached;

	@BeforeEach void fixture() {
		properties = new BffSecurityProperties(); properties.setSharedKey(key);
		filter = new BffAuthenticationFilter(properties, new ObjectMapper());
		response = new MockHttpServletResponse(); reached = new AtomicBoolean();
	}

	@Test void localHttpWithMatchingKeyPasses() throws Exception {
		run(request("/api/v1/health", key));
		assertThat(reached.get()).isTrue();
	}
	@Test void missingOrWrongKeyIsRejectedEvenOnPublicLogin() throws Exception {
		run(request("/api/v1/auth/social/login", "wrong"));
		assertThat(reached.get()).isFalse(); assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("BFF_UNAUTHORIZED").doesNotContain(key);
	}
	@Test void missingKeyCannotReachHealth() throws Exception {
		run(request("/api/v1/health", null));
		assertThat(reached.get()).isFalse(); assertThat(response.getStatus()).isEqualTo(403);
	}
	@Test void duplicateKeyHeadersAreRejected() throws Exception {
		var request = request("/api/v1/health", key); request.addHeader("X-BFF-Key", "another"); run(request);
		assertThat(reached.get()).isFalse();
	}
	@Test void productionRejectsHttpEvenWithCorrectKey() throws Exception {
		properties.setRequireHttps(true); run(request("/api/v1/health", key));
		assertThat(reached.get()).isFalse(); assertThat(response.getContentAsString()).contains("HTTPS_REQUIRED");
	}
	@Test void productionAcceptsRealTls() throws Exception {
		properties.setRequireHttps(true); var request = request("/api/v1/health", key); request.setSecure(true); run(request);
		assertThat(reached.get()).isTrue();
	}
	@Test void forgedForwardedProtocolFromUntrustedPeerIsRejected() throws Exception {
		properties.setRequireHttps(true); properties.setTrustedProxies(List.of("127.0.0.1/32"));
		var request = request("/api/v1/health", key); request.setRemoteAddr("192.0.2.20");
		request.addHeader("X-Forwarded-Proto", "https"); request.addHeader("Forwarded", "proto=https"); run(request);
		assertThat(reached.get()).isFalse();
	}
	@Test void trustedTlsTerminatorIsAccepted() throws Exception {
		properties.setRequireHttps(true); properties.setTrustedProxies(List.of("127.0.0.1/32"));
		var request = request("/api/v1/health", key); request.setRemoteAddr("127.0.0.1");
		request.addHeader("X-Forwarded-Proto", "https"); run(request);
		assertThat(reached.get()).isTrue();
	}
	@Test void trustedProxyCannotSubmitAmbiguousProtocolChain() throws Exception {
		properties.setRequireHttps(true); properties.setTrustedProxies(List.of("127.0.0.1/32"));
		var request = request("/api/v1/health", key); request.addHeader("X-Forwarded-Proto", "https, http"); run(request);
		assertThat(reached.get()).isFalse();
	}
	@Test void contextPathDoesNotBypassKeyCheck() throws Exception {
		var request = request("/app/api/v1/auth/login", null); request.setContextPath("/app"); run(request);
		assertThat(reached.get()).isFalse();
	}
	@Test void websocketHandshakeUsesTicketButItsRestTicketEndpointNeedsKey() throws Exception {
		var request = request("/api/v1/ws", null); request.addHeader("Upgrade", "websocket"); run(request);
		assertThat(reached.get()).isTrue();
		reached.set(false); run(request("/api/v1/ws/ticket", null)); assertThat(reached.get()).isFalse();
	}
	@Test void productionWebsocketAlsoRequiresTls() throws Exception {
		properties.setRequireHttps(true);
		var request = request("/api/v1/ws", null); request.addHeader("Upgrade", "websocket"); run(request);
		assertThat(reached.get()).isFalse(); assertThat(response.getContentAsString()).contains("HTTPS_REQUIRED");
	}
	@Test void staticUploadPathsAreOutsideBffRestGuard() throws Exception {
		run(request("/uploads/public.jpg", null)); assertThat(reached.get()).isTrue();
	}

	MockHttpServletRequest request(String path, String header) {
		var request = new MockHttpServletRequest("GET", path); request.setRemoteAddr("127.0.0.1");
		if (header != null) request.addHeader("X-BFF-Key", header);
		return request;
	}
	void run(MockHttpServletRequest request) throws Exception {
		filter.doFilter(request, response, (req, res) -> reached.set(true));
	}
}
