package me._hanho.ultary.domain.pet.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePetRequest {

	@NotBlank(message = "이름은 필수입니다.")
	@Size(max = 30, message = "이름은 30자 이하여야 합니다.")
	private String name;

	@Pattern(regexp = "DOG|CAT|ETC", message = "species는 DOG, CAT, ETC만 가능합니다.")
	private String species;

	@Size(max = 50, message = "품종은 50자 이하여야 합니다.")
	private String breed;

	@Pattern(regexp = "MALE|FEMALE|UNKNOWN", message = "gender는 MALE, FEMALE, UNKNOWN만 가능합니다.")
	private String gender;

	private Boolean isNeutered;

	private LocalDateTime birthday;

	/** 사전 업로드한 프로필 이미지 fileId */
	private Long profileFileId;

	@Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
	private String bio;
}
