package me._hanho.ultary.domain.settings.dto.request;

import lombok.Getter;
import lombok.Setter;

/** 넣은 항목만 바꾼다 */
@Getter
@Setter
public class UpdatePrivacyRequest {

	private Boolean privateAccount;
	private String feedVisibility;
	private String storyVisibility;
	private Boolean neighborRequest;
	private Boolean allowComment;
	private Boolean allowMention;
	private Boolean allowTag;

	public boolean hasAny() {
		return privateAccount != null
				|| feedVisibility != null
				|| storyVisibility != null
				|| neighborRequest != null
				|| allowComment != null
				|| allowMention != null
				|| allowTag != null;
	}
}
