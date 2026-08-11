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
	/** uploadDir 기준 상대 경로 (예: images/uuid.jpg) */
	private String filePath;
	private Long uploadedByUserNo;
	private LocalDateTime createdAt;
}
