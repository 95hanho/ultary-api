package me._hanho.ultary.common.response;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String code,
		String message,
		T data,
		Instant timestamp) {

	/** code는 항상 OK. message로 동작 결과를 구분한다. */
	public static <T> ApiResponse<T> ok(T data, String message) {
		return new ApiResponse<>(true, "OK", message, data, Instant.now());
	}

	public static <T> ApiResponse<T> ok(T data) {
		return ok(data, "요청이 성공했습니다.");
	}

	/** data 없는 성공 (String data용 ok과 시그니처 충돌 방지) */
	public static ApiResponse<Void> okEmpty(String message) {
		return new ApiResponse<>(true, "OK", message, null, Instant.now());
	}

	public static ApiResponse<Void> ok() {
		return okEmpty("요청이 성공했습니다.");
	}
}
