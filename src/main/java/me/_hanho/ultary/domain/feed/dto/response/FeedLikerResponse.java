package me._hanho.ultary.domain.feed.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedLikerResponse {

	private Long userNo;
	private String nickname;
	private LocalDateTime likedAt;
}
