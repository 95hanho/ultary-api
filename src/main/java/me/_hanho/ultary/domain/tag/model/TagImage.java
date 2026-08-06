package me._hanho.ultary.domain.tag.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_tag_image */
@Getter
@Setter
public class TagImage {

	private Long tagImageId;
	private Long tagId;
	private Long fileId;
	private Integer sortOrder;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
