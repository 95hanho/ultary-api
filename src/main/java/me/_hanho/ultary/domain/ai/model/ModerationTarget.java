package me._hanho.ultary.domain.ai.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationTarget {
    private Long targetId;
    private Long userNo;
    private Long feedId;
    private Long parentCommentId;
    private String content;
    private boolean deleted;
}
