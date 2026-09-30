package me._hanho.ultary.domain.story.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

	/** 사진 위 글자. 생략하면 없음. 최대 20개. 배열 순서가 쌓이는 순서 */
	@Size(max = 20, message = "글자는 최대 20개입니다.")
	@Valid
	private List<TextItem> texts;

	/** 사진 위 @펫. petId는 활성 펫과 1:1. 최대 20개 */
	@Size(max = 20, message = "멘션은 최대 20개입니다.")
	@Valid
	private List<MentionItem> mentions;

	@Getter
	@Setter
	public static class TextItem {

		@NotBlank(message = "글자 내용은 필수입니다.")
		@Size(max = 200, message = "글자는 200자 이하여야 합니다.")
		private String content;

		/** 12 | 16 | 20 | 24. 생략 시 16 */
		private Integer fontSize;

		private Boolean bold;

		private Boolean underline;

		private Boolean strikethrough;

		@NotBlank(message = "글자 색상은 필수입니다.")
		@Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "글자 색상은 #RRGGBB 형식이어야 합니다.")
		private String color;

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
}
