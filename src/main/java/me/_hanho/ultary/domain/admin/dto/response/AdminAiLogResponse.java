package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Operational metadata only; excludes content snapshots, prompts and provider bodies. */
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
}
