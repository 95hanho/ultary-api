package me._hanho.ultary.domain.file.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.dto.response.FileResponse;
import me._hanho.ultary.domain.file.mapper.FileMapper;
import me._hanho.ultary.domain.file.model.FileMeta;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

	private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
	private static final long VIDEO_MAX_BYTES = 50L * 1024 * 1024;

	private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

	@Value("${spring.servlet.multipart.location}")
	private String uploadDir;

	private final FileMapper fileMapper;

	/**
	 * 이미지/영상 업로드. mediaTypeHint: IMAGE | VIDEO | AUTO(또는 null)
	 */
	@Transactional
	public FileResponse upload(MultipartFile file, String mediaTypeHint, Long userNo) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.FILE_EMPTY);
		}

		String originalName = file.getOriginalFilename();
		if (!StringUtils.hasText(originalName)) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}

		String extension = extractExt(originalName).toLowerCase(Locale.ROOT);
		MediaKind kind = resolveMediaKind(mediaTypeHint, extension, file.getContentType());
		validateSize(kind, file.getSize());
		validateMime(kind, file.getContentType());

		String storeName = UUID.randomUUID() + "." + extension;
		String relativePath = kind.subdir + "/" + storeName;
		Path absolutePath = resolveAbsolutePath(relativePath);

		saveToDisk(file, absolutePath);

		FileMeta meta = new FileMeta();
		meta.setOriginalName(truncate(originalName, 100));
		meta.setStoreName(storeName);
		meta.setExtension(extension);
		meta.setMimeType(file.getContentType());
		meta.setFileSize(toIntSize(file.getSize()));
		meta.setFilePath(relativePath);
		meta.setUploadedByUserNo(userNo);

		fileMapper.insert(meta);
		log.info("[upload] fileId={} kind={} size={} path={}", meta.getFileId(), kind, meta.getFileSize(), relativePath);
		return toResponse(meta);
	}

	@Transactional(readOnly = true)
	public FileResponse getMeta(Long fileId) {
		return toResponse(requireActive(fileId));
	}

	@Transactional(readOnly = true)
	public Resource getContent(Long fileId) {
		FileMeta meta = requireActive(fileId);
		Path path = resolveAbsolutePath(meta.getFilePath());
		if (!Files.isRegularFile(path)) {
			log.warn("[getContent] disk missing fileId={} path={}", fileId, path);
			throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
		}
		return new FileSystemResource(path);
	}

	@Transactional(readOnly = true)
	public FileMeta requireActive(Long fileId) {
		FileMeta meta = fileMapper.findActiveByFileId(fileId);
		if (meta == null) {
			throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
		}
		return meta;
	}

	@Transactional
	public void softDelete(Long fileId, Long userNo) {
		FileMeta meta = requireActive(fileId);
		if (userNo != null
				&& meta.getUploadedByUserNo() != null
				&& !meta.getUploadedByUserNo().equals(userNo)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		int updated = fileMapper.softDelete(fileId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
		}
		log.info("[softDelete] fileId={} userNo={}", fileId, userNo);
	}

	private MediaKind resolveMediaKind(String hint, String extension, String contentType) {
		String normalized = hint == null ? "AUTO" : hint.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "IMAGE" -> {
				if (!IMAGE_EXTENSIONS.contains(extension)) {
					throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
				}
				yield MediaKind.IMAGE;
			}
			case "VIDEO" -> {
				if (!VIDEO_EXTENSIONS.contains(extension)) {
					throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
				}
				yield MediaKind.VIDEO;
			}
			case "AUTO", "" -> {
				if (IMAGE_EXTENSIONS.contains(extension)
						|| (contentType != null && contentType.startsWith("image/"))) {
					if (!IMAGE_EXTENSIONS.contains(extension)) {
						throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
					}
					yield MediaKind.IMAGE;
				}
				if (VIDEO_EXTENSIONS.contains(extension)
						|| (contentType != null && contentType.startsWith("video/"))) {
					if (!VIDEO_EXTENSIONS.contains(extension)) {
						throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
					}
					yield MediaKind.VIDEO;
				}
				throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
			}
			default -> throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "mediaType는 IMAGE, VIDEO, AUTO만 가능합니다.");
		};
	}

	private void validateSize(MediaKind kind, long size) {
		long max = kind == MediaKind.IMAGE ? IMAGE_MAX_BYTES : VIDEO_MAX_BYTES;
		if (size > max) {
			throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
		}
	}

	private void validateMime(MediaKind kind, String contentType) {
		if (contentType == null) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}
		if (kind == MediaKind.IMAGE && !contentType.startsWith("image/")) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}
		if (kind == MediaKind.VIDEO && !contentType.startsWith("video/")) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}
	}

	private void saveToDisk(MultipartFile file, Path absolutePath) {
		try {
			Files.createDirectories(absolutePath.getParent());
			file.transferTo(absolutePath);
		} catch (IllegalStateException | IOException e) {
			log.error("[saveToDisk] path={} error={}", absolutePath, e.getMessage());
			throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
		}
	}

	private Path resolveAbsolutePath(String relativePath) {
		return Paths.get(uploadDir).resolve(relativePath).normalize();
	}

	private String extractExt(String originalFileName) {
		int pos = originalFileName.lastIndexOf('.');
		if (pos < 0 || pos == originalFileName.length() - 1) {
			throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
		}
		return originalFileName.substring(pos + 1);
	}

	private int toIntSize(long size) {
		if (size > Integer.MAX_VALUE) {
			throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
		}
		return (int) size;
	}

	private String truncate(String value, int max) {
		if (value == null || value.length() <= max) {
			return value;
		}
		return value.substring(0, max);
	}

	private FileResponse toResponse(FileMeta meta) {
		return FileResponse.builder()
				.fileId(meta.getFileId())
				.originalName(meta.getOriginalName())
				.storeName(meta.getStoreName())
				.extension(meta.getExtension())
				.mimeType(meta.getMimeType())
				.fileSize(meta.getFileSize())
				.filePath(meta.getFilePath())
				.uploadedByUserNo(meta.getUploadedByUserNo())
				.createdAt(meta.getCreatedAt())
				.build();
	}

	private enum MediaKind {
		IMAGE("images"),
		VIDEO("videos");

		private final String subdir;

		MediaKind(String subdir) {
			this.subdir = subdir;
		}
	}
}
