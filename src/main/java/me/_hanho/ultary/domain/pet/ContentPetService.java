package me._hanho.ultary.domain.pet;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Service
@RequiredArgsConstructor
public class ContentPetService {
    private final ContentPetMapper mapper;
    private final PetMapper petMapper;
    private final FileService fileService;

    public record SubjectPet(Long petId, String name, FileSummaryResponse profileFile) {}

    @Transactional
    public void replace(String type, Long id, Long owner, List<Long> petIds) {
        if (petIds == null) return;
        if (petIds.size() > 20 || petIds.stream().anyMatch(java.util.Objects::isNull))
            throw new BusinessException(ErrorCode.INVALID_INPUT, "반려동물 선택이 올바르지 않습니다.");
        var unique = new LinkedHashSet<>(petIds);
        for (Long petId : unique) {
            if (petMapper.findActiveByPetIdAndUserNo(petId, owner) == null)
                throw new BusinessException(ErrorCode.INVALID_INPUT, "본인의 등록된 반려동물만 선택할 수 있습니다.");
        }
        mapper.delete(type, id);
        int sort = 0;
        for (Long petId : unique) mapper.insert(type, id, petId, sort++);
    }

    @Transactional(readOnly = true)
    public List<SubjectPet> list(String type, Long id) {
        return toSubjects(mapper.find(type, id));
    }

    @Transactional(readOnly = true)
    public List<SubjectPet> storyOwnerPets(Long owner) {
        return toSubjects(mapper.findStoryOwnerPets(owner));
    }

    private List<SubjectPet> toSubjects(List<me._hanho.ultary.domain.pet.model.Pet> pets) {
        var fileIds = pets.stream().filter(p -> p.getProfileFileId() != null)
            .map(p -> p.getProfileFileId().longValue()).toList();
        var files = fileService.findSummaries(fileIds);
        return pets.stream().map(p -> new SubjectPet(p.getPetId(), p.getName(),
            p.getProfileFileId() == null ? null : files.get(p.getProfileFileId().longValue()))).toList();
    }
}
