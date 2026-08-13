package me._hanho.ultary.domain.feed.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedResponse {

	private Long feedId;
	private Long userNo;
	private String authorNickname;
	private String content;
	private String visibility;
	private Integer likeCount;
	private Integer commentCount;
	private Integer storeCount;
	private Boolean likedByMe;
	private Boolean storedByMe;
	private List<MediaItem> media;
	private List<PetItem> pets;
	private List<Long> tagIds;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Getter
	@Builder
	public static class MediaItem {
		private Long feedMediaId;
		private Long fileId;
		private String mediaType;
		private Long thumbnailFileId;
		private Integer durationSec;
		private Integer sortOrder;
		private List<MentionItem> mentions;
	}

	@Getter
	@Builder
	public static class MentionItem {
		private Long petId;
		private BigDecimal posX;
		private BigDecimal posY;
	}

	@Getter
	@Builder
	public static class PetItem {
		private Long petId;
		private String role;
		private Boolean isMain;
		private Long addedByUserNo;
	}
}
