package me._hanho.ultary.domain.tag.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** hashtag·handle 제외. null 필드는 유지 */
@Getter
@Setter
public class UpdateTagRequest {

	@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
	private String title;

	@Size(max = 500, message = "소개글은 500자 이하여야 합니다.")
	private String content;

	@Size(max = 200, message = "링크는 200자 이하여야 합니다.")
	private String link;

	/** true면 title을 NULL로 클리어 */
	private Boolean clearTitle;

	/** true면 content를 NULL로 클리어 */
	private Boolean clearContent;

	/** true면 link를 NULL로 클리어 */
	private Boolean clearLink;
}
