package me._hanho.ultary.domain.file.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileResponse {

	private Long fileId;
	private String originalName;
	private String storeName;
	private String extension;
	private String mimeType;
	private Integer fileSize;
	/**
	 * uploadDir 기준 상대 경로({@code images/uuid.jpg}) 또는 CDN 절대 URL.
	 * 목록 임베드는 {@link FileSummaryResponse} 참고.
	 */
	private String filePath;
	/** OWNED | UNSPLASH | AI | ETC */
	private String sourceType;
	private String authorName;
	private String sourceUrl;
	private String licenseUrl;
	private String copyrightNotice;
	private Long uploadedByUserNo;
	private LocalDateTime createdAt;
}
