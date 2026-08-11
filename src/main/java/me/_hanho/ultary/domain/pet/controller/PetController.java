package me._hanho.ultary.domain.pet.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.pet.dto.request.UpdatePetRequest;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.domain.pet.service.PetService;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.pets / api-memo.md §4 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

	private final PetService petService;

	/** 내 반려동물 목록 */
	@GetMapping
	public ApiResponse<List<PetResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
		log.info("[list] userNo={}", principal.getUserNo());
		return ApiResponse.ok(petService.list(principal));
	}

	/** 반려동물 등록 */
	@PostMapping
	public ApiResponse<PetResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreatePetRequest request) {
		log.info("[create] name={} species={}", request.getName(), request.getSpecies());
		return ApiResponse.ok(petService.create(principal, request));
	}

	/** 반려동물 수정 */
	@PatchMapping("/{petId}")
	public ApiResponse<PetResponse> update(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long petId,
			@Valid @RequestBody UpdatePetRequest request) {
		log.info("[update] petId={}", petId);
		return ApiResponse.ok(petService.update(principal, petId, request));
	}

	/** 반려동물 삭제 (soft delete) */
	@DeleteMapping("/{petId}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long petId) {
		log.info("[delete] petId={}", petId);
		petService.delete(principal, petId);
		return ApiResponse.ok();
	}

	/** 피드 반려동물 태그 승인 (보호자) */
	@PostMapping("/tags/{feedPetId}/approve")
	public ApiResponse<Void> approveTag(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedPetId) {
		log.info("[approveTag] feedPetId={}", feedPetId);
		petService.approveTag(principal, feedPetId);
		return ApiResponse.ok();
	}

	/** 피드 반려동물 태그 거절 (보호자) */
	@PostMapping("/tags/{feedPetId}/reject")
	public ApiResponse<Void> rejectTag(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedPetId) {
		log.info("[rejectTag] feedPetId={}", feedPetId);
		petService.rejectTag(principal, feedPetId);
		return ApiResponse.ok();
	}
}
