package me._hanho.ultary.domain.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.model.FeedPet;
import me._hanho.ultary.domain.file.service.FileService;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.pet.dto.request.UpdatePetRequest;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.domain.pet.mapper.PetMapper;
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

		Pet pet = new Pet();
		pet.setUserNo(principal.getUserNo());
		pet.setName(request.getName().trim());
		pet.setSpecies(normalizeOrDefault(request.getSpecies(), "DOG"));
		pet.setBreed(blankToNull(request.getBreed()));
		pet.setGender(normalizeOrDefault(request.getGender(), "UNKNOWN"));
		pet.setIsNeutered(request.getIsNeutered() != null && request.getIsNeutered());
		pet.setBirthday(request.getBirthday());
		pet.setProfileFileId(request.getProfileFileId());
		pet.setBio(blankToNull(request.getBio()));

		petMapper.insert(pet);
		log.info("[create] petId={} userNo={}", pet.getPetId(), principal.getUserNo());
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
		if (request.getName() != null) {
			if (!StringUtils.hasText(request.getName())) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "이름은 비울 수 없습니다.");
			}
			patch.setName(request.getName().trim());
		}
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

	@Transactional
	public void delete(UserPrincipal principal, Long petId) {
		int deleted = petMapper.softDelete(petId, principal.getUserNo());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[delete] petId={} userNo={}", petId, principal.getUserNo());
	}

	@Transactional
	public void approveTag(UserPrincipal principal, Long feedPetId) {
		FeedPet tag = requirePendingOwnedTag(feedPetId, principal.getUserNo());
		int updated = petMapper.approveFeedPetTag(tag.getFeedPetId(), principal.getUserNo());
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_TAG_ALREADY_PROCESSED);
		}
		log.info("[approveTag] feedPetId={} userNo={}", feedPetId, principal.getUserNo());
	}

	@Transactional
	public void rejectTag(UserPrincipal principal, Long feedPetId) {
		FeedPet tag = requirePendingOwnedTag(feedPetId, principal.getUserNo());
		int updated = petMapper.rejectFeedPetTag(tag.getFeedPetId());
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_TAG_ALREADY_PROCESSED);
		}
		log.info("[rejectTag] feedPetId={} userNo={}", feedPetId, principal.getUserNo());
	}

	private FeedPet requirePendingOwnedTag(Long feedPetId, Long userNo) {
		FeedPet tag = petMapper.findFeedPetOwnedByUser(feedPetId, userNo);
		if (tag == null) {
			throw new BusinessException(ErrorCode.PET_TAG_NOT_FOUND);
		}
		if (!"PENDING".equals(tag.getStatus())) {
			throw new BusinessException(ErrorCode.PET_TAG_ALREADY_PROCESSED);
		}
		return tag;
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
