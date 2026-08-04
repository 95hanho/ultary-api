package me._hanho.ultary.security.handler;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.ErrorCode;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ErrorCode errorCode = ErrorCode.FORBIDDEN;
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				errorCode.getHttpStatus(),
				errorCode.getMessage());
		problemDetail.setProperty("code", errorCode.getCode());
		problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));

		objectMapper.writeValue(response.getWriter(), problemDetail);
	}
}
