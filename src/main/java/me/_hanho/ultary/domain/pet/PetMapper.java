package me._hanho.ultary.domain.pet;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.pet.model.RepresentativePetProfile;

@Mapper
public interface PetMapper {

	List<Pet> findActiveByUserNo(@Param("userNo") Long userNo);

	Pet findActiveByPetId(@Param("petId") Long petId);

	Pet findActiveByPetIdAndUserNo(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo);

	int insert(Pet pet);

	int update(Pet pet);

	int updateMentionId(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo,
			@Param("mentionId") String mentionId);

	/** local 테스트: 멘션 ID 쿨다운 기준 시각만 바꾼다 */
	int updateMentionIdChangedAt(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo,
			@Param("mentionIdChangedAt") LocalDateTime mentionIdChangedAt);

	int softDelete(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo);

	int countByMentionId(
			@Param("mentionId") String mentionId,
			@Param("excludePetId") Long excludePetId);

	int countActiveByUserNo(@Param("userNo") Long userNo);

	Integer findMaxPriority(@Param("userNo") Long userNo);

	Integer findRepresentativeProfileFileId(@Param("userNo") Long userNo);

	List<RepresentativePetProfile> findRepresentativeProfiles(
			@Param("userNos") Collection<Long> userNos);

	List<Pet> searchActive(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);
}
