package me._hanho.ultary.domain.myultary.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyBioRequest {

	@Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
	private String bio;

	/** true면 bio를 NULL로 클리어 */
	private Boolean clearBio;
}
