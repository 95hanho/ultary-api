package me._hanho.ultary.domain.ai;

/**
 * DB {@code ultary_ai_request_log.feature_type} ENUM 값.
 * 비즈니스 이름(TAG_RECOMMEND 등)과 스키마 ENUM을 맞춘다. 스키마 변경 없이 매핑.
 */
public final class AiFeatureTypes {

	/** 게시글 내용 해시태그 추천 → DB HASHTAG */
	public static final String HASHTAG = "HASHTAG";

	/** 게시글 본문 유해 콘텐츠 필터 → DB FEED_CAPTION */
	public static final String FEED_CAPTION = "FEED_CAPTION";

	/** 댓글·답글 유해 콘텐츠 필터 → DB COMMENT_FILTER */
	public static final String COMMENT_FILTER = "COMMENT_FILTER";

	private AiFeatureTypes() {
	}
}
