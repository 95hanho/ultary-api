package me._hanho.ultary.domain.ai.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationState {
    private String targetType;
    private Long targetId;
    private long latestVersion;
    private long approvedVersion;
    private String approvedContent;
    private boolean hasApproved;
}
