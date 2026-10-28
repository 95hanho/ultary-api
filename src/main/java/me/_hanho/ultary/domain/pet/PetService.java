package me._hanho.ultary.domain.pet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.pet.dto.request.UpdatePetRequest;
import me._hanho.ultary.domain.pet.dto.response.PetResponse;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.pet.model.RepresentativePetProfile;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

	private final PetMapper petMapper;
    private final me._hanho.ultary.domain.user.UserMapper userMapper;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public List<PetResponse> list(UserPrincipal principal) {
		return listByOwner(principal.getUserNo());
	}

	/** 그 유저의 활성 펫. 정렬은 내 목록과 같다 */
	@Transactional(readOnly = true)
	public List<PetResponse> listByOwner(Long ownerUserNo) {
		return petMapper.findActiveByUserNo(ownerUserNo).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public PetResponse create(UserPrincipal principal, CreatePetRequest request) {
        if (userMapper.lockActiveUser(principal.getUserNo()) == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        if (petMapper.countActiveByUserNo(principal.getUserNo()) >= 3) throw new BusinessException(ErrorCode.INVALID_INPUT, "반려동물은 계정당 최대 3마리까지 등록할 수 있습니다.");
        validateProfileFile(request.getProfileFileId());
        String mentionId = "pet_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);

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
		pet.setPriority(resolveCreatePriority(principal.getUserNo(), request.getPriority()));
		pet.setBio(blankToNull(request.getBio()));

		petMapper.insert(pet);
		log.info("[create] petId={} userNo={}", pet.getPetId(), principal.getUserNo());
		return toResponse(requireOwned(pet.getPetId(), principal.getUserNo()));
	}

	@Transactional
	public PetResponse update(UserPrincipal principal, Long petId, UpdatePetRequest request) {
		Pet existing = requireOwned(petId, principal.getUserNo());
		Long previousProfileFileId = existing.getProfileFileId();

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
		if (request.getPriority() != null) {
			patch.setPriority(request.getPriority());
		}

		int updated = petMapper.update(patch);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		if (profileFileReplaced(previousProfileFileId, request, removeProfile)) {
			fileService.releaseIfUnused(previousProfileFileId);
		}
		log.info("[update] petId={} userNo={}", petId, principal.getUserNo());
		return toResponse(requireOwned(existing.getPetId(), principal.getUserNo()));
	}

	private static boolean profileFileReplaced(Long previousProfileFileId, UpdatePetRequest request, boolean removeProfile) {
		if (previousProfileFileId == null) {
			return false;
		}
		if (removeProfile) {
			return true;
		}
		return request.getProfileFileId() != null
				&& !request.getProfileFileId().equals(previousProfileFileId);
	}

	@Transactional
	public void delete(UserPrincipal principal, Long petId) {
		int deleted = petMapper.softDelete(petId, principal.getUserNo());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		log.info("[delete] petId={} userNo={}", petId, principal.getUserNo());
	}

	/** 관리 순서 첫 번째 활성 펫의 profile_file_id. 사진이 없으면 null (다음 펫으로 대체하지 않음) */
	@Transactional(readOnly = true)
	public Integer representativeProfileFileId(Long userNo) {
		if (userNo == null) {
			return null;
		}
		return petMapper.findRepresentativeProfileFileId(userNo);
	}

	@Transactional(readOnly = true)
	public Map<Long, Integer> representativeProfileFileIds(Collection<Long> userNos) {
		if (userNos == null || userNos.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = userNos.stream().filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<Long, Integer> result = new HashMap<>();
		for (RepresentativePetProfile row : petMapper.findRepresentativeProfiles(ids)) {
			if (row.getUserNo() != null && row.getProfileFileId() != null) {
				result.put(row.getUserNo(), row.getProfileFileId());
			}
		}
		return result;
	}

    @Transactional(readOnly = true)
    public Map<Long, List<String>> petNamesByUser(Collection<Long> userNos) {
        if (userNos == null || userNos.isEmpty()) return Map.of();
        Map<Long, List<String>> names = new HashMap<>();
        for (var pet : petMapper.findActiveMentions(userNos.stream().distinct().toList())) {
            names.computeIfAbsent(pet.getUserNo(), key -> new java.util.ArrayList<>()).add(pet.getName());
        }
        return names;
    }

	private Integer resolveCreatePriority(Long userNo, Integer requested) {
		if (requested != null) {
			return requested;
		}
		Integer max = petMapper.findMaxPriority(userNo);
		return max == null ? 1 : max + 1;
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
				.profileFile(fileService.findSummary(pet.getProfileFileId()))
				.priority(pet.getPriority())
				.bio(pet.getBio())
				.createdAt(pet.getCreatedAt())
				.updatedAt(pet.getUpdatedAt())
				.build();
	}
}
