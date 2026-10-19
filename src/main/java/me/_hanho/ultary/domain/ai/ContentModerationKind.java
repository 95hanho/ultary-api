package me._hanho.ultary.domain.ai;

/** Gemini 유해 콘텐츠 검사 대상 */
public enum ContentModerationKind {
	/** 게시글 본문 → DB FEED_CAPTION */
	FEED,
	/** 댓글 → DB COMMENT_FILTER */
	COMMENT,
	/** 답글 → DB COMMENT_FILTER */
	REPLY
}
