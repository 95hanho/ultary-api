package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 타임라인 UNION 조회용 중간 행(파일·펫은 서비스에서 채움) */
@Getter
@Setter
public class AdminTimelineRow {
	private String type;
	private Long id;
	private LocalDateTime occurredAt;
	private String message;
	private String content;
	private Long fileId;
	private Long thumbnailFileId;
	private String petName;
}
