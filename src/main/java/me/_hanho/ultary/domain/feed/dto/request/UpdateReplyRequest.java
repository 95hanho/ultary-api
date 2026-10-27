package me._hanho.ultary.domain.feed.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReplyRequest {

	@NotBlank(message = "답글 내용은 필수입니다.")
	@Size(max = 100, message = "답글은 100자 이하여야 합니다.")
	private String content;

	@Valid
	@Size(max = 20)
	private List<CommentMentionRequest> mentions;
}
