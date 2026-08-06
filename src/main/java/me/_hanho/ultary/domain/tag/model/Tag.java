package me._hanho.ultary.domain.tag.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_tag */
@Getter
@Setter
public class Tag {

	private Long tagId;
	private String name;
	private String title;
	private String content;
	private String link;
	private Integer useCount;
	private Long createdByUserNo;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;
}
