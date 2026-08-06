package me._hanho.ultary.domain.pet.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.pet.service.PetService;

/** 경로 원본: springEndpoints.pets / api-memo.md §4 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

	private final PetService petService;

	// 반려동물 목록
	@GetMapping
	public ApiResponse<Void> list() {
		log.info("[list]");
		petService.list();
		return ApiResponse.ok();
	}

	// 반려동물 등록
	@PostMapping
	public ApiResponse<Void> create() {
		log.info("[create]");
		petService.create();
		return ApiResponse.ok();
	}

	// 반려동물 수정
	@PatchMapping("/{petId}")
	public ApiResponse<Void> update(@PathVariable Long petId) {
		log.info("[update] petId={}", petId);
		petService.update(petId);
		return ApiResponse.ok();
	}

	// 반려동물 삭제
	@DeleteMapping("/{petId}")
	public ApiResponse<Void> delete(@PathVariable Long petId) {
		log.info("[delete] petId={}", petId);
		petService.delete(petId);
		return ApiResponse.ok();
	}

	// 피드 반려동물 태그 승인
	@PostMapping("/tags/{feedPetId}/approve")
	public ApiResponse<Void> approveTag(@PathVariable Long feedPetId) {
		log.info("[approveTag] feedPetId={}", feedPetId);
		petService.approveTag(feedPetId);
		return ApiResponse.ok();
	}

	// 피드 반려동물 태그 거절
	@PostMapping("/tags/{feedPetId}/reject")
	public ApiResponse<Void> rejectTag(@PathVariable Long feedPetId) {
		log.info("[rejectTag] feedPetId={}", feedPetId);
		petService.rejectTag(feedPetId);
		return ApiResponse.ok();
	}
}
