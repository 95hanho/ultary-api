package me._hanho.ultary.domain.story.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStoryRequest {

	@NotNull(message = "fileId는 필수입니다.")
	private Long fileId;

	/** IMAGE | VIDEO. 생략 시 파일 mime/확장자로 추론 */
	@Pattern(regexp = "IMAGE|VIDEO", message = "mediaType은 IMAGE 또는 VIDEO만 가능합니다.")
	private String mediaType;

	private Long thumbnailFileId;

	@Min(value = 1, message = "영상 길이는 1초 이상이어야 합니다.")
	@Max(value = 60, message = "영상 길이는 60초 이하여야 합니다.")
	private Integer durationSec;

	@Size(max = 200, message = "캡션은 200자 이하여야 합니다.")
	private String caption;
}
