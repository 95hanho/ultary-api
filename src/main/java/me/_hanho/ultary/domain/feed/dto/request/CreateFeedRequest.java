package me._hanho.ultary.domain.feed.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFeedRequest {

	@Size(max = 1000, message = "본문은 1000자 이하여야 합니다.")
	private String content;

	@Pattern(regexp = "PUBLIC|NEIGHBORS|PRIVATE", message = "visibility는 PUBLIC, NEIGHBORS, PRIVATE만 가능합니다.")
	private String visibility;

	@NotEmpty(message = "미디어는 1개 이상 필요합니다.")
	@Size(max = 10, message = "미디어는 최대 10개입니다.")
	@Valid
	private List<MediaItem> media;

	@Size(max = 20, message = "반려동물은 최대 20마리입니다.")
	@Valid
	private List<PetItem> pets;

	@Size(max = 30, message = "태그는 최대 30개입니다.")
	private List<Long> tagIds;

	@Getter
	@Setter
	public static class MediaItem {

		@NotNull(message = "fileId는 필수입니다.")
		private Long fileId;

		/** IMAGE | VIDEO. 생략 시 파일 mime/확장자로 추론 */
		@Pattern(regexp = "IMAGE|VIDEO", message = "mediaType은 IMAGE 또는 VIDEO만 가능합니다.")
		private String mediaType;

		private Long thumbnailFileId;

		private Integer durationSec;

		@Size(max = 20, message = "미디어 멘션은 최대 20개입니다.")
		@Valid
		private List<MentionItem> mentions;
	}

	@Getter
	@Setter
	public static class MentionItem {

		@NotNull(message = "petId는 필수입니다.")
		private Long petId;

		@NotNull(message = "posX는 필수입니다.")
		@DecimalMin(value = "0.00", message = "posX는 0 이상이어야 합니다.")
		@DecimalMax(value = "100.00", message = "posX는 100 이하여야 합니다.")
		private BigDecimal posX;

		@NotNull(message = "posY는 필수입니다.")
		@DecimalMin(value = "0.00", message = "posY는 0 이상이어야 합니다.")
		@DecimalMax(value = "100.00", message = "posY는 100 이하여야 합니다.")
		private BigDecimal posY;
	}

	@Getter
	@Setter
	public static class PetItem {

		@NotNull(message = "petId는 필수입니다.")
		private Long petId;

		@Pattern(regexp = "TAGGED|COLLABORATOR", message = "role은 TAGGED 또는 COLLABORATOR만 가능합니다.")
		private String role;

		private Boolean isMain;
	}
}
