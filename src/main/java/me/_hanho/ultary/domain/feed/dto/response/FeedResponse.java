package me._hanho.ultary.domain.feed.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.report.dto.response.MyReportResponse;

@Getter
@Builder
public class FeedResponse {

	private Long feedId;
	private Long userNo;
	private String authorNickname;
	/** 작성자 프로필. 미등록·삭제 파일이면 null */
	private FileSummaryResponse authorProfileFile;
	private String content;
	private String visibility;
	private Integer likeCount;
	private Integer commentCount;
	private Integer pinCount;
	private Boolean likedByMe;
	private Boolean pinnedByMe;
	/** 나만 보는 저장. 다른 사람에게는 목록이 없다 */
	private Boolean savedByMe;
	/** 내 신고. 없으면 null. status가 REQUESTED면 취소 가능 */
	private MyReportResponse myReport;
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
		private FileSummaryResponse file;
		private String mediaType;
		private Long thumbnailFileId;
		private FileSummaryResponse thumbnailFile;
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
