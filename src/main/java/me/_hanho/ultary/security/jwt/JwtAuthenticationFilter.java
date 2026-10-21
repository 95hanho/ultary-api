package me._hanho.ultary.security.jwt;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.admin.AdminMapper;
import me._hanho.ultary.domain.admin.model.Admin;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.AdminPrincipal;
import me._hanho.ultary.security.principal.UserPrincipal;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserMapper userMapper;
	private final AdminMapper adminMapper;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);

		if (!StringUtils.hasText(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		String path = requestPath(request);
		if (isAdminApi(path)) {
			if (isAdminPublicAuth(request, path)) {
				filterChain.doFilter(request, response);
				return;
			}
			authenticateAdmin(token, response, filterChain, request);
			return;
		}

		try {
			Claims claims = jwtTokenProvider.parseUserAccessToken(token);
			Long userNo = jwtTokenProvider.getSubjectAsLong(claims);

			User user = userMapper.findActiveByUserNo(userNo);
			if (user == null) {
				writeClosedAccount(response, userNo);
				return;
			}

			UserPrincipal principal = new UserPrincipal(user.getUserNo(), user.getNickname());
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (BusinessException ex) {
			writeUnauthorized(response, ex.getErrorCode());
		}
	}

	private void authenticateAdmin(
			String token,
			HttpServletResponse response,
			FilterChain filterChain,
			HttpServletRequest request) throws IOException, ServletException {
		try {
			Claims claims = jwtTokenProvider.parseAdminAccessToken(token);
			Long adminNo = jwtTokenProvider.getSubjectAsLong(claims);
			Admin admin = adminMapper.findByAdminNo(adminNo);
			if (admin == null || !"ACTIVE".equals(admin.getStatus())) {
				writeClosedAdmin(response, admin);
				return;
			}

			AdminPrincipal principal = new AdminPrincipal(admin.getAdminNo(), admin.getLoginId(), admin.getRole());
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (BusinessException ex) {
			writeUnauthorized(response, ex.getErrorCode());
		}
	}

	private void writeClosedAdmin(HttpServletResponse response, Admin admin) throws IOException {
		if (admin != null && "SUSPENDED".equals(admin.getStatus())) {
			writeUnauthorized(response, ErrorCode.ACCOUNT_SUSPENDED);
			return;
		}
		writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
	}

	private boolean isAdminApi(String path) {
		return path.startsWith("/api/v1/admin");
	}

	private boolean isAdminPublicAuth(HttpServletRequest request, String path) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		return "/api/v1/admin/auth/login".equals(path) || "/api/v1/admin/auth/refresh".equals(path)
				|| "/api/v1/admin/auth/logout".equals(path);
	}

	private String requestPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		if (StringUtils.hasText(context) && uri.startsWith(context)) {
			return uri.substring(context.length());
		}
		return uri;
	}

	private void writeClosedAccount(HttpServletResponse response, Long userNo) throws IOException {
		User user = userMapper.findByUserNo(userNo);
		if (user != null && "WITHDRAWN".equals(user.getWithdrawalStatus())) {
			writeUnauthorized(response, ErrorCode.ACCOUNT_WITHDRAWN);
			return;
		}
		if (user != null && "SUSPENDED".equals(user.getWithdrawalStatus())) {
			writeUnauthorized(response, ErrorCode.ACCOUNT_SUSPENDED);
			return;
		}
		writeUnauthorized(response, ErrorCode.UNAUTHORIZED);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}
		return null;
	}

	private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		SecurityContextHolder.clearContext();
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		var problemDetail = org.springframework.http.ProblemDetail.forStatusAndDetail(
				errorCode.getHttpStatus(),
				errorCode.getMessage());
		problemDetail.setProperty("code", errorCode.getCode());
		problemDetail.setProperty("message", errorCode.getMessage());

		objectMapper.writeValue(response.getWriter(), problemDetail);
	}
}
