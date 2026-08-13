package me._hanho.ultary.domain.feed.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** content·visibility만. 미디어/펫/태그 교체는 별도 정책 전까지 미지원 */
@Getter
@Setter
public class UpdateFeedRequest {

	@Size(max = 1000, message = "본문은 1000자 이하여야 합니다.")
	private String content;

	@Pattern(regexp = "PUBLIC|NEIGHBORS|PRIVATE", message = "visibility는 PUBLIC, NEIGHBORS, PRIVATE만 가능합니다.")
	private String visibility;

	/** true면 content를 NULL로 클리어 */
	private Boolean clearContent;
}
