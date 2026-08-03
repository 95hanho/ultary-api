package me._hanho.ultary.common.exception;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusinessException(
			BusinessException ex,
			HttpServletRequest request) {
		ErrorCode errorCode = ex.getErrorCode();
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				errorCode.getHttpStatus(),
				ex.getMessage());
		enrichProblemDetail(problemDetail, errorCode.getCode(), request.getRequestURI());
		return ResponseEntity.status(errorCode.getHttpStatus()).body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid",
						(existing, replacement) -> existing));

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				ErrorCode.INVALID_INPUT.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.INVALID_INPUT.getCode(), request.getRequestURI());
		problemDetail.setProperty("errors", fieldErrors);
		return ResponseEntity.badRequest().body(problemDetail);
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

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
			NoResourceFoundException ex,
			HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND,
				ErrorCode.NOT_FOUND.getMessage());
		enrichProblemDetail(problemDetail, ErrorCode.NOT_FOUND.getCode(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleException(
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
		problemDetail.setInstance(URI.create(instance));
	}
}
