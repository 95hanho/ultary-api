package me._hanho.ultary.domain.settings.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrivacyResponse {

	private boolean privateAccount;
	/** PUBLIC | NEIGHBORS | PRIVATE. 새 게시글 기본값 */
	private String feedVisibility;
	/** PUBLIC | NEIGHBORS | PRIVATE */
	private String storyVisibility;
	private boolean neighborRequest;
	private boolean allowComment;
	private boolean allowMention;
	private boolean allowTag;
}
