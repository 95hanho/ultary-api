package me._hanho.ultary.domain.pet.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PetResponse {

	private Long petId;
	private Long userNo;
	private String mentionId;
	private LocalDateTime mentionIdChangedAt;
	private String name;
	private String species;
	private String breed;
	private String gender;
	private boolean neutered;
	private LocalDateTime birthday;
	private Long profileFileId;
	private String bio;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
