package me._hanho.ultary.domain.tag.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/** ultary_tag */
@Getter
@Setter
public class Tag {

	private Long tagId;
	/** # 입력용 (중복 가능, 생성 후 불변) */
	private String hashtag;
	/** USER 일반 해시태그, COMMERCIAL 상업 태그 */
	private String purpose;
	/** NONE, PENDING, APPROVED, REJECTED. APPROVED일 때만 설명이 공개된다 */
	private String profileStatus;
	/** 상품/소개 표시명. 승인된 상업 태그만 조회에 나온다 */
	private String title;
	/** 선택 고유 코드 (영문·숫자·_) */
	private String handle;
	private LocalDateTime handleChangedAt;
	private String content;
	private String link;
	private Integer useCount;
	private Long createdByUserNo;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Boolean isDeleted;
	private LocalDateTime deletedAt;

	/** update용: title NULL 클리어 */
	private Boolean clearTitle;
	/** update용: content NULL 클리어 */
	private Boolean clearContent;
	/** update용: link NULL 클리어 */
	private Boolean clearLink;
}
