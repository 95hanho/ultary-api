package me._hanho.ultary.domain.pet.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.HandleRules;

@Getter
@Setter
public class CreatePetRequest {

	@NotBlank(message = "멘션 ID는 필수입니다.")
	@Pattern(regexp = HandleRules.REGEX, message = HandleRules.MENTION_MESSAGE)
	private String mentionId;

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

	/** 없으면 내 펫 맨 뒤. 작을수록 우선, 1이 가장 높음 */
	@Min(value = 1, message = "우선순위는 1 이상이어야 합니다.")
	private Integer priority;

	@Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
	private String bio;
}
