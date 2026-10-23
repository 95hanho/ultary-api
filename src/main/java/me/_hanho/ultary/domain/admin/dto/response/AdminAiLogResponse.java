package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 관리자 운영 정보. 상세에서만 검사 본문을 포함하며 프롬프트·제공자 응답 원문은 제외한다. */
@Getter
@Setter
public class AdminAiLogResponse {
    private Long aiRequestId;
    private Long userNo;
    private String featureType;
    private Long targetFeedId;
    private String targetType;
    private Long targetId;
    private Long moderationJobId;
    private Long contentVersion;
    private Integer attemptNo;
    private String modelName;
    private String status;
    private String errorCode;
    private String errorMessage;
    private Long latencyMs;
    private Integer httpStatus;
    private LocalDateTime createdAt;
    /** Current job state, not its historical state at the time of this attempt. */
    private String jobStatus;
    private String resultAction;
    private Integer jobAttemptCount;
    private LocalDateTime nextAttemptAt;
    private String adminReviewStatus;
    private LocalDateTime adminReviewedAt;
    private String inspectionContent;
}
