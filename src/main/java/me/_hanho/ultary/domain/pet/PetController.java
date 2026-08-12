package me._hanho.ultary.domain.pet;

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
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.domain.pet.dto.request.ChangeMentionIdRequest;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.pet.dto.request.UpdatePetRequest;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.domain.pet.PetService;
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

	/** 반려동물 등록 (name·mentionId는 이후 정책에 따라 변경 제한) */
	@PostMapping
	public ApiResponse<PetResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreatePetRequest request) {
		log.info("[create] mentionId={} name={}", request.getMentionId(), request.getName());
		return ApiResponse.ok(petService.create(principal, request));
	}

	/** mention_id 변경 가능 여부 */
	@GetMapping("/{petId}/mention-id/change-availability")
	public ApiResponse<ChangeAvailabilityResponse> mentionIdChangeAvailability(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long petId) {
		log.info("[mentionIdChangeAvailability] petId={}", petId);
		return ApiResponse.ok(petService.mentionIdChangeAvailability(principal, petId));
	}

	/** mention_id 변경 (생성·변경 후 30일 쿨다운). name은 변경 불가 */
	@PatchMapping("/{petId}/mention-id")
	public ApiResponse<PetResponse> changeMentionId(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long petId,
			@Valid @RequestBody ChangeMentionIdRequest request) {
		log.info("[changeMentionId] petId={} mentionId={}", petId, request.getMentionId());
		return ApiResponse.ok(petService.changeMentionId(principal, petId, request));
	}

	/** 반려동물 수정 (name·mentionId 제외) */
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
}
