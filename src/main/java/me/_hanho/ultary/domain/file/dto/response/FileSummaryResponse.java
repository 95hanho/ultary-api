package me._hanho.ultary.domain.file.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 목록·상세 응답에 임베드하는 파일 요약.
 * {@code filePath}는 uploadDir 상대경로({@code images/…}) 또는 CDN 절대 URL.
 */
@Getter
@Builder
public class FileSummaryResponse {

	private Long fileId;
	private String filePath;
	private String mimeType;
	private String extension;
	/** OWNED | UNSPLASH | AI | ETC */
	private String sourceType;
	private String authorName;
	private String sourceUrl;
	private String licenseUrl;
	private String copyrightNotice;
}
