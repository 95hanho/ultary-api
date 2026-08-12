package me._hanho.ultary.domain.pet;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.pet.model.Pet;

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

	int softDelete(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo);

	int countByMentionId(
			@Param("mentionId") String mentionId,
			@Param("excludePetId") Long excludePetId);
}
