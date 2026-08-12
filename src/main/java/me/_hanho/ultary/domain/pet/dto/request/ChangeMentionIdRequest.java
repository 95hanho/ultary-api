package me._hanho.ultary.domain.pet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.common.validation.HandleRules;

@Getter
@Setter
public class ChangeMentionIdRequest {

	@NotBlank(message = "멘션 ID는 필수입니다.")
	@Pattern(regexp = HandleRules.REGEX, message = HandleRules.MESSAGE)
	private String mentionId;
}
