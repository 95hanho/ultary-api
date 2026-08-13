package me._hanho.ultary.domain.write;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;

/** 경로 원본: springEndpoints.write / api-memo.md §8 */
@Slf4j
@RestController
@RequestMapping("/api/v1/write")
@RequiredArgsConstructor
public class WriteController {

	private final WriteService writeService;

	@PostMapping("/draft-photos")
	public ApiResponse<Void> draftPhotos() {
		log.info("[draftPhotos]");
		writeService.saveDraftPhotos();
		return ApiResponse.okEmpty("작성 사진 임시저장 성공");
	}

	@PostMapping("/edited-photos")
	public ApiResponse<Void> editedPhotos() {
		log.info("[editedPhotos]");
		writeService.saveEditedPhotos();
		return ApiResponse.okEmpty("사진 편집본 저장 성공");
	}

	@PostMapping("/photo-tags")
	public ApiResponse<Void> photoTags() {
		log.info("[photoTags]");
		writeService.savePhotoTags();
		return ApiResponse.okEmpty("사진 태그 저장 성공");
	}
}
