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
	private Integer likeCount;
	private Integer commentCount;
	private Integer storeCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
