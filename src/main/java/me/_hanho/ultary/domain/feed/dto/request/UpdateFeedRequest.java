package me._hanho.ultary.domain.feed.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest.MentionItem;
import lombok.Getter;
import lombok.Setter;

/** Existing media stay fixed; only content, visibility and their tags may change. */
@Getter
@Setter
public class UpdateFeedRequest {
    /** null은 기존 선택 유지, 빈 배열은 선택 해제. */
    @Size(max = 20)
    private List<@NotNull @Positive Long> subjectPetIds;

	@Size(max = 100, message = "본문은 100자 이하여야 합니다.")
	private String content;

	@Pattern(regexp = "PUBLIC|NEIGHBORS|PRIVATE", message = "visibility는 PUBLIC, NEIGHBORS, PRIVATE만 가능합니다.")
	private String visibility;

	/** true면 content를 NULL로 클리어 */
	private Boolean clearContent;

	/** null preserves photo tags; each supplied media's mentions are replaced (empty clears). */
	@Size(max = 10)
	@Valid
	private List<@NotNull MediaTags> mediaTags;

	/** null preserves hashtag links; empty clears them. */
	@Size(max = 30)
	private List<@NotNull @Positive Long> tagIds;

	@Getter
	@Setter
	public static class MediaTags {
		@NotNull @Positive
		private Long feedMediaId;
		@NotNull @Size(max = 20) @Valid
		private List<@NotNull MentionItem> mentions;
	}
}
