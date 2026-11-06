package me._hanho.ultary.domain.feed.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_feed */
@Getter
@Setter
public class Feed {

	private Long feedId;
	private Long userNo;
	private String content;
	/** PUBLIC | NEIGHBORS | PRIVATE */
	private String visibility;
	/** null이면 DB 기본 true. false면 작성자만 댓글·답글 가능 */
	private Boolean allowComment;
	private Integer likeCount;
	private Integer commentCount;
	private Integer pinCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
	/** 작성자 또는 COLLABORATOR 펫 보호자 */
	private Long deletedByUserNo;

	/** update용: content NULL 클리어 */
	private Boolean clearContent;
}
