package me._hanho.ultary.domain.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.feed.model.FeedPet;
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

	int softDelete(
			@Param("petId") Long petId,
			@Param("userNo") Long userNo);

	FeedPet findFeedPetOwnedByUser(
			@Param("feedPetId") Long feedPetId,
			@Param("userNo") Long userNo);

	int approveFeedPetTag(
			@Param("feedPetId") Long feedPetId,
			@Param("approvedByUserNo") Long approvedByUserNo);

	int rejectFeedPetTag(@Param("feedPetId") Long feedPetId);
}
