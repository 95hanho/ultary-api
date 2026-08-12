package me._hanho.ultary.domain.tag.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import me._hanho.ultary.common.validation.HandleRules;
import me._hanho.ultary.common.validation.HashtagRules;

@Getter
public class CreateTagRequest {

	/** # 없이 전달 권장. 앞에 #가 있어도 서버에서 제거 */
	@NotBlank(message = "해시태그는 필수입니다.")
	@Size(max = 30, message = "해시태그는 30자 이하여야 합니다.")
	@Pattern(regexp = HashtagRules.REGEX, message = HashtagRules.MESSAGE)
	private String hashtag;

	@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
	private String title;

	@Pattern(regexp = HandleRules.REGEX, message = HandleRules.MESSAGE)
	private String handle;

	@Size(max = 500, message = "소개글은 500자 이하여야 합니다.")
	private String content;

	@Size(max = 200, message = "링크는 200자 이하여야 합니다.")
	private String link;

	/** 사전 업로드한 소개 이미지 fileId 목록 (순서 = sort_order) */
	@Size(max = 10, message = "태그 이미지는 최대 10장입니다.")
	private List<Long> imageFileIds;

	public void setHashtag(String hashtag) {
		this.hashtag = HashtagRules.normalize(hashtag);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setHandle(String handle) {
		if (handle == null || handle.isBlank()) {
			this.handle = null;
		} else {
			this.handle = handle.trim();
		}
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public void setImageFileIds(List<Long> imageFileIds) {
		this.imageFileIds = imageFileIds;
	}
}
