package me._hanho.ultary.domain.ai.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationJob {
    private Long jobId;
    private String targetType;
    private Long targetId;
    private Long feedId;
    private Long userNo;
    private Long contentVersion;
    private String operation;
    private String content;
    private String status;
    private int attemptCount;
    private String claimToken;
    private String lastErrorCode;
    private String lastError;
    private String resultAction;
    private int retryDelay;
}
