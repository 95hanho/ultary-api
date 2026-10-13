package me._hanho.ultary.domain.tag.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 상업 태그에 올릴 제목·소개·링크·이미지. 관리자 승인 후에만 공개된다. */
@Getter
@Setter
public class SubmitTagProfileRequest {

	@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
	private String title;

	@Size(max = 500, message = "소개글은 500자 이하여야 합니다.")
	private String content;

	@Size(max = 200, message = "링크는 200자 이하여야 합니다.")
	private String link;

	@Size(max = 10, message = "태그 이미지는 최대 10장입니다.")
	private List<Long> imageFileIds;
}
