package me._hanho.ultary.domain.file.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_file */
@Getter
@Setter
public class FileMeta {

	private Long fileId;
	private String originalName;
	private String storeName;
	private String extension;
	private String mimeType;
	private Integer fileSize;
	private String filePath;
	private String copyright;
	private String copyrightUrl;
	private Long uploadedByUserNo;
	private Long uploadedByAdminNo;
	private LocalDateTime createdAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
