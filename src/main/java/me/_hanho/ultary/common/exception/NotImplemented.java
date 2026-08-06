package me._hanho.ultary.common.exception;

/**
 * API 스켈레톤용. 실제 로직 구현 전 501 반환.
 * 경로 원본: ultary-web docs/api-memo.md, springEndpoints
 */
public final class NotImplemented {

	private NotImplemented() {
	}

	public static <T> T yet() {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
	}
}
