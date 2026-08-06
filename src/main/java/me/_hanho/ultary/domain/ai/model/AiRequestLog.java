package me._hanho.ultary.domain.ai.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_ai_request_log */
@Getter
@Setter
public class AiRequestLog {

	private Long aiRequestId;
	private Long userNo;
	/** FEED_CAPTION | HASHTAG | PET_PROFILE | COMMENT_FILTER | ALT_TEXT */
	private String featureType;
	private Long targetFeedId;
	private Long targetPetId;
	private String prompt;
	private String result;
	private String modelName;
	private Integer usedTokenCount;
	/** PENDING | SUCCESS | FAILED */
	private String status;
	private String errorMessage;
	private LocalDateTime createdAt;
}
