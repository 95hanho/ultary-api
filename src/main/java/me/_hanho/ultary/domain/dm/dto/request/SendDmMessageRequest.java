package me._hanho.ultary.domain.dm.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendDmMessageRequest {

	/** 같이 보내는 글. 공유만 할 때는 비워도 된다 */
	private String body;
	private Long feedId;
	/** 게시글 캐러셀에서 공유한 사진. 없으면 첫 장 */
	private Long feedMediaId;
	private Long storyId;
}
