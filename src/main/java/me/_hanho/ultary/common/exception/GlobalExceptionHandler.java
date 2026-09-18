package me._hanho.ultary.common.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;

/**
 * {@code spring.mvc.problemdetails.enabled=true} 일 때 Boot 기본 검증 응답을 덮어쓴다.
 * {@link ResponseEntityExceptionHandler}가 이미 매핑한 예외는 {@code @ExceptionHandler}로
 * 중복 선언하지 말고 {@code override} 한다 (Ambiguous 방지).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(ApiResponse.fail(errorCode.getCode(), ex.getMessage()));
	}

	/** signup 등 {@code @Valid} 실패 → ApiResponse (Invalid request content. 대체) */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		Map<String, String> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid",
						(existing, replacement) -> existing,
						LinkedHashMap::new));

		String message = fieldErrors.values().stream()
				.findFirst()
				.orElse(ErrorCode.INVALID_INPUT.getMessage());
		log.warn("[validation] INVALID_INPUT fields={} message={}", fieldErrors.keySet(), message);
		return ResponseEntity
				.badRequest()
				.body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message, fieldErrors));
	}

	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(
			NoResourceFoundException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND,
				ErrorCode.NOT_FOUND.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.NOT_FOUND.getCode(), requestUri(request));
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> handleAuthenticationException(
			AuthenticationException ex,
			HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED,
				ErrorCode.UNAUTHORIZED.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.UNAUTHORIZED.getCode(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDeniedException(
			AccessDeniedException ex,
			HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.FORBIDDEN,
				ErrorCode.FORBIDDEN.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.FORBIDDEN.getCode(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnhandled(
			Exception ex,
			HttpServletRequest request) {
		log.error("Unhandled exception", ex);
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
	}

	private void enrichProblemDetail(ProblemDetail problemDetail, String code, String instance) {
		problemDetail.setProperty("code", code);
		problemDetail.setProperty("message", problemDetail.getDetail());
		problemDetail.setInstance(URI.create(instance));
	}

	private static String requestUri(WebRequest request) {
		if (request instanceof ServletWebRequest servletWebRequest) {
			return servletWebRequest.getRequest().getRequestURI();
		}
		return "/";
	}
}
