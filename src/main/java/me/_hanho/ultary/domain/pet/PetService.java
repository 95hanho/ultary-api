package me._hanho.ultary.domain.pet;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.pet.dto.request.ChangeMentionIdRequest;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.pet.dto.request.UpdatePetRequest;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

	private final PetMapper petMapper;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public List<PetResponse> list(UserPrincipal principal) {
		return petMapper.findActiveByUserNo(principal.getUserNo()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public PetResponse create(UserPrincipal principal, CreatePetRequest request) {
		validateProfileFile(request.getProfileFileId());
		String mentionId = request.getMentionId().trim();
		ensureMentionAvailable(mentionId, null);

		Pet pet = new Pet();
		pet.setUserNo(principal.getUserNo());
		pet.setMentionId(mentionId);
		pet.setName(request.getName().trim());
		pet.setSpecies(normalizeOrDefault(request.getSpecies(), "DOG"));
		pet.setBreed(blankToNull(request.getBreed()));
		pet.setGender(normalizeOrDefault(request.getGender(), "UNKNOWN"));
		pet.setIsNeutered(request.getIsNeutered() != null && request.getIsNeutered());
		pet.setBirthday(request.getBirthday());
		pet.setProfileFileId(request.getProfileFileId());
		pet.setBio(blankToNull(request.getBio()));

		petMapper.insert(pet);
		log.info("[create] petId={} mentionId={} userNo={}", pet.getPetId(), mentionId, principal.getUserNo());
		return toResponse(requireOwned(pet.getPetId(), principal.getUserNo()));
	}

	@Transactional
	public PetResponse update(UserPrincipal principal, Long petId, UpdatePetRequest request) {
		Pet existing = requireOwned(petId, principal.getUserNo());

		boolean removeProfile = Boolean.TRUE.equals(request.getRemoveProfileFile());
		if (!removeProfile && request.getProfileFileId() != null) {
			validateProfileFile(request.getProfileFileId());
		}

		Pet patch = new Pet();
		patch.setPetId(petId);
		patch.setUserNo(principal.getUserNo());
		if (request.getSpecies() != null) {
			patch.setSpecies(request.getSpecies());
		}
		if (request.getBreed() != null) {
			patch.setBreed(request.getBreed().trim());
		}
		if (request.getGender() != null) {
			patch.setGender(request.getGender());
		}
		if (request.getIsNeutered() != null) {
			patch.setIsNeutered(request.getIsNeutered());
		}
		if (request.getBirthday() != null) {
			patch.setBirthday(request.getBirthday());
		}
		if (request.getBio() != null) {
			patch.setBio(request.getBio().trim());
		}
		patch.setClearProfileFile(removeProfile);
		if (!removeProfile && request.getProfileFileId() != null) {
			patch.setProfileFileId(request.getProfileFileId());
		}

		int updated = petMapper.update(patch);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[update] petId={} userNo={}", petId, principal.getUserNo());
		return toResponse(requireOwned(existing.getPetId(), principal.getUserNo()));
	}

	@Transactional(readOnly = true)
	public ChangeAvailabilityResponse mentionIdChangeAvailability(UserPrincipal principal, Long petId) {
		Pet pet = requireOwned(petId, principal.getUserNo());
		return IdentityChangeCooldown.availability(
				pet.getMentionIdChangedAt(),
				IdentityChangeCooldown.MENTION_ID_DAYS);
	}

	@Transactional
	public PetResponse changeMentionId(UserPrincipal principal, Long petId, ChangeMentionIdRequest request) {
		Pet pet = requireOwned(petId, principal.getUserNo());
		String mentionId = request.getMentionId().trim();
		if (mentionId.equalsIgnoreCase(pet.getMentionId())) {
			return toResponse(pet);
		}

		IdentityChangeCooldown.requireChangeable(
				pet.getMentionIdChangedAt(),
				IdentityChangeCooldown.MENTION_ID_DAYS,
				ErrorCode.PET_MENTION_CHANGE_COOLDOWN);

		ensureMentionAvailable(mentionId, petId);

		int updated = petMapper.updateMentionId(petId, principal.getUserNo(), mentionId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[changeMentionId] petId={} mentionId={}", petId, mentionId);
		return toResponse(requireOwned(petId, principal.getUserNo()));
	}

	@Transactional
	public void delete(UserPrincipal principal, Long petId) {
		int deleted = petMapper.softDelete(petId, principal.getUserNo());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[delete] petId={} userNo={}", petId, principal.getUserNo());
	}

	private void ensureMentionAvailable(String mentionId, Long excludePetId) {
		if (petMapper.countByMentionId(mentionId, excludePetId) > 0) {
			throw new BusinessException(ErrorCode.PET_MENTION_DUPLICATED);
		}
	}

	private Pet requireOwned(Long petId, Long userNo) {
		Pet pet = petMapper.findActiveByPetIdAndUserNo(petId, userNo);
		if (pet == null) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		return pet;
	}

	private void validateProfileFile(Long profileFileId) {
		if (profileFileId != null) {
			fileService.requireActive(profileFileId);
		}
	}

	private String normalizeOrDefault(String value, String defaultValue) {
		return StringUtils.hasText(value) ? value.trim().toUpperCase() : defaultValue;
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private PetResponse toResponse(Pet pet) {
		return PetResponse.builder()
				.petId(pet.getPetId())
				.userNo(pet.getUserNo())
				.mentionId(pet.getMentionId())
				.mentionIdChangedAt(pet.getMentionIdChangedAt())
				.name(pet.getName())
				.species(pet.getSpecies())
				.breed(pet.getBreed())
				.gender(pet.getGender())
				.neutered(Boolean.TRUE.equals(pet.getIsNeutered()))
				.birthday(pet.getBirthday())
				.profileFileId(pet.getProfileFileId())
				.bio(pet.getBio())
				.createdAt(pet.getCreatedAt())
				.updatedAt(pet.getUpdatedAt())
				.build();
	}
}
