package me._hanho.ultary.security.bff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** BFF 서버 인증은 사용자 JWT 인증과 별개이며 먼저 검사한다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BffAuthenticationFilter extends OncePerRequestFilter {

	public static final String KEY_HEADER = "X-BFF-Key";
	private final BffSecurityProperties properties;
	private final ObjectMapper json;
	private final AtomicBoolean httpsWarningLogged = new AtomicBoolean();

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = path(request);
		return !path.equals("/api/v1") && !path.startsWith("/api/v1/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (properties.isRequireHttps() && !secureTransport(request)) {
			if ("/api/v1/health/db".equals(path(request)) && httpsWarningLogged.compareAndSet(false, true)) {
				var protocols = Collections.list(request.getHeaders("X-Forwarded-Proto"));
				log.warn("[BFF] HTTPS_REQUIRED: remoteAddr={}, scheme={}, secure={}, forwardedProtoCount={}, forwardedProtoHttps={}, trustedProxyCount={}",
						request.getRemoteAddr(), request.getScheme(), request.isSecure(), protocols.size(),
						protocols.size() == 1 && "https".equalsIgnoreCase(protocols.get(0)), properties.getTrustedProxies().size());
			}
			reject(response, "HTTPS_REQUIRED", "HTTPS 통신이 필요합니다.");
			return;
		}
		// 브라우저 WebSocket은 키 대신 기존의 일회용 티켓으로 인증한다. 하위 ticket API는 제외하지 않는다.
		boolean socketHandshake = "/api/v1/ws".equals(path(request)) && "GET".equals(request.getMethod())
				&& "websocket".equalsIgnoreCase(request.getHeader("Upgrade"));
		if (!socketHandshake) {
			var keys = Collections.list(request.getHeaders(KEY_HEADER));
			String expected = properties.getSharedKey();
			if (keys.size() != 1 || expected == null || expected.length() < 32 || keys.get(0).length() > 256
					|| !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), keys.get(0).getBytes(StandardCharsets.UTF_8))) {
				reject(response, "BFF_UNAUTHORIZED", "BFF 서버 인증에 실패했습니다.");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private boolean secureTransport(HttpServletRequest request) {
		if (request.isSecure()) return true;
		boolean trusted = properties.getTrustedProxies().stream()
				.filter(value -> value != null && !value.isBlank())
				.anyMatch(value -> new IpAddressMatcher(value.trim()).matches(request.getRemoteAddr()));
		if (!trusted) return false;
		var protocols = Collections.list(request.getHeaders("X-Forwarded-Proto"));
		return protocols.size() == 1 && "https".equalsIgnoreCase(protocols.get(0));
	}

	private String path(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		return !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
	}

	private void reject(HttpServletResponse response, String code, String detail) throws IOException {
		response.setStatus(403);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatus.FORBIDDEN, detail);
		problem.setProperty("code", code);
		problem.setProperty("message", detail);
		json.writeValue(response.getWriter(), problem);
	}
}
