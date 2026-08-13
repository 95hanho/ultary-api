package me._hanho.ultary.domain.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCommentRequest {

	@NotBlank(message = "댓글 내용은 필수입니다.")
	@Size(max = 500, message = "댓글은 500자 이하여야 합니다.")
	private String content;
}
