package me._hanho.ultary.domain.main.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 스토리 @ 또는 사진 태그에서 고른 펫 */
@Getter
@Setter
public class SavePetTagHistoryRequest {

	@NotNull(message = "petId는 필수입니다.")
	private Long petId;
}
