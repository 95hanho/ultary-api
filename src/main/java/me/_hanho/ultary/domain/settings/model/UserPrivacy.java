package me._hanho.ultary.domain.settings.model;

import lombok.Getter;
import lombok.Setter;

/** ultary_user_privacy. 행이 없으면 기본값 */
@Getter
@Setter
public class UserPrivacy {

	private Long userNo;
	private Boolean privateAccount;
	private String feedVisibility;
	private String storyVisibility;
	private Boolean neighborRequest;
	private Boolean allowComment;
	private Boolean allowMention;
	private Boolean allowTag;
}
