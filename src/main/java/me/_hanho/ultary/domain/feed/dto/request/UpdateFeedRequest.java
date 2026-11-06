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

/** Media files stay fixed; content, visibility, tags, and media order may change. */
@Getter
@Setter
public class UpdateFeedRequest {
    /** null은 기존 선택 유지, 빈 배열은 선택 해제. */
    @Size(max = 20)
    private List<@NotNull @Positive Long> subjectPetIds;

	@Size(max = 500, message = "본문은 500자 이하여야 합니다.")
	private String content;

	@Pattern(regexp = "PUBLIC|NEIGHBORS|PRIVATE", message = "visibility는 PUBLIC, NEIGHBORS, PRIVATE만 가능합니다.")
	private String visibility;

	/** 생략 시 기존 값 유지. false면 작성자만 댓글·답글 가능 */
	private Boolean allowComment;

	/** true면 content를 NULL로 클리어 */
	private Boolean clearContent;

	/** null preserves photo tags; each supplied media's mentions are replaced (empty clears). */
	@Size(max = 10)
	@Valid
	private List<@NotNull MediaTags> mediaTags;

	/**
	 * null/생략이면 기존 sortOrder 유지.
	 * 전달 시 이 게시글의 활성 feedMediaId 전체와 집합이 같아야 하며, 배열 인덱스가 새 sortOrder다.
	 */
	@Size(max = 10)
	private List<@NotNull @Positive Long> mediaOrder;

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
