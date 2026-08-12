package me._hanho.ultary.domain.pet.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** null 필드는 변경하지 않음. name/mentionId는 전용 정책 — 이 API에서 변경 불가 */
@Getter
@Setter
public class UpdatePetRequest {

	@Pattern(regexp = "DOG|CAT|ETC", message = "species는 DOG, CAT, ETC만 가능합니다.")
	private String species;

	@Size(max = 50, message = "품종은 50자 이하여야 합니다.")
	private String breed;

	@Pattern(regexp = "MALE|FEMALE|UNKNOWN", message = "gender는 MALE, FEMALE, UNKNOWN만 가능합니다.")
	private String gender;

	private Boolean isNeutered;

	private LocalDateTime birthday;

	private Long profileFileId;

	/** true면 profile_file_id를 NULL로 클리어 */
	private Boolean removeProfileFile;

	@Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
	private String bio;
}
