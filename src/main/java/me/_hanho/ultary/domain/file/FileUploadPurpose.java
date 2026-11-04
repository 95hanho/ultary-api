package me._hanho.ultary.domain.file;

import java.util.Locale;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;

/**
 * 업로드 저장 목적. uploadDir 아래 {@code images/{folder}/} · {@code videos/{folder}/} 에 저장한다.
 * yml의 uploadDir 자체는 바꾸지 않는다.
 */
public enum FileUploadPurpose {

	FEED("feed"),
	STORY("story"),
	PET("pet"),
	TAG("tag");

	private final String folder;

	FileUploadPurpose(String folder) {
		this.folder = folder;
	}

	public String folder() {
		return folder;
	}

	/**
	 * null/blank → FEED. 그 외 FEED|STORY|PET|TAG (대소문자 무시).
	 */
	public static FileUploadPurpose fromParam(String raw) {
		if (raw == null || raw.isBlank()) {
			return FEED;
		}
		try {
			return FileUploadPurpose.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_INPUT,
					"purpose는 FEED, STORY, PET, TAG만 가능합니다.");
		}
	}
}
