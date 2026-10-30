package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminActionLogResponse {
    private Long actionLogId;
    private Long adminNo;
    private String adminLoginId;
    private String adminName;
    private String action;
    private String targetType;
    private Long targetId;
    private Long requestId;
    private String targetLabel;
    private String beforeStatus;
    private String afterStatus;
    private String reason;
    private boolean sample;
    private LocalDateTime createdAt;
}
