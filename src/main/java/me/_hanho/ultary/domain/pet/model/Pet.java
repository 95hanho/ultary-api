package me._hanho.ultary.domain.pet.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_pet */
@Getter
@Setter
public class Pet {

	private Long petId;
	private Long userNo;
	private String name;
	/** DOG | CAT | ETC */
	private String species;
	private String breed;
	/** MALE | FEMALE | UNKNOWN */
	private String gender;
	private Boolean isNeutered;
	private LocalDateTime birthday;
	private Integer profileFileId;
	private String bio;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
