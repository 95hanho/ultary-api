package me._hanho.ultary.domain.feed.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

/** 댓글/답글 @멘션. userNo / petId 중 정확히 하나 */
@Getter
@Setter
public class CommentMentionRequest {

	private Long userNo;
	private Long petId;

	@AssertTrue(message = "멘션은 userNo 또는 petId 중 하나만 지정해야 합니다.")
	public boolean isValidTarget() {
		boolean hasUser = userNo != null;
		boolean hasPet = petId != null;
		return hasUser != hasPet;
	}
}
