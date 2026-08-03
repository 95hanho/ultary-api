package me._hanho.ultary.common.response;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String code,
		String message,
		T data,
		Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data, Instant.now());
	}

	public static ApiResponse<Void> ok() {
		return ok(null);
	}
}
