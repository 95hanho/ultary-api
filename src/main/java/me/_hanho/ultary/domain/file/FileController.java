package me._hanho.ultary.domain.file;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.file.dto.response.FileResponse;
import me._hanho.ultary.domain.file.model.FileMeta;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.security.principal.UserPrincipal;

/**
 * 공통 파일 API. Feed/Pet 등은 FileService를 직접 주입해도 되고, 클라이언트가 이 API로 fileId를 받아 넘겨도 됨.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

	private final FileService fileService;

	/** 파일 업로드 (IMAGE / VIDEO / AUTO) */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<FileResponse> upload(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "mediaType", required = false, defaultValue = "AUTO") String mediaType) {
		log.info("[upload] mediaType={} originalName={} size={}",
				mediaType, file.getOriginalFilename(), file.getSize());
		return ApiResponse.ok(fileService.upload(file, mediaType, principal.getUserNo()));
	}

	/** 파일 메타 조회 */
	@GetMapping("/{fileId}")
	public ApiResponse<FileResponse> meta(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long fileId) {
		log.info("[meta] fileId={} userNo={}", fileId, principal.getUserNo());
		return ApiResponse.ok(fileService.getMeta(fileId));
	}

	/** 파일 바이너리 다운로드/미리보기 */
	@GetMapping("/{fileId}/content")
	public ResponseEntity<Resource> content(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long fileId) {
		log.info("[content] fileId={} userNo={}", fileId, principal.getUserNo());
		FileMeta meta = fileService.requireActive(fileId);
		Resource resource = fileService.getContent(fileId);
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (meta.getMimeType() != null) {
			try {
				mediaType = MediaType.parseMediaType(meta.getMimeType());
			} catch (Exception ignored) {
				// keep octet-stream
			}
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getStoreName() + "\"")
				.contentType(mediaType)
				.body(resource);
	}

	/** 소프트 삭제 (업로더 본인만) */
	@DeleteMapping("/{fileId}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long fileId) {
		log.info("[delete] fileId={} userNo={}", fileId, principal.getUserNo());
		fileService.softDelete(fileId, principal.getUserNo());
		return ApiResponse.ok();
	}
}
