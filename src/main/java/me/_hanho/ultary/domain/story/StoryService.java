package me._hanho.ultary.domain.story;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.file.model.FileMeta;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.domain.story.dto.response.StoryLikeResponse;
import me._hanho.ultary.domain.story.dto.response.StoryMentionResponse;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.story.dto.response.StoryTextResponse;
import me._hanho.ultary.domain.story.model.Story;
import me._hanho.ultary.domain.story.model.StoryLike;
import me._hanho.ultary.domain.story.model.StoryMentionRow;
import me._hanho.ultary.domain.story.model.StoryText;
import me._hanho.ultary.domain.story.model.StoryOwnerRow;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
	private static final Set<Integer> FONT_SIZES = Set.of(12, 16, 20, 24);

	private final StoryMapper storyMapper;
	private final FileService fileService;
	private final UserMapper userMapper;
	private final PetService petService;
	private final PetMapper petMapper;
	private final NotificationService notificationService;
	private final PrivacyService privacyService;

	@Transactional
	public StoryResponse create(UserPrincipal principal, CreateStoryRequest request) {
		FileMeta file = fileService.requireActive(request.getFileId());
		String mediaType = resolveMediaType(request.getMediaType(), file);
		Long thumbnailFileId = request.getThumbnailFileId();
		Integer durationSec = request.getDurationSec();
		if ("IMAGE".equals(mediaType)) {
			thumbnailFileId = null;
			durationSec = null;
		} else {
			if (thumbnailFileId != null) {
				fileService.requireActive(thumbnailFileId);
			}
			if (durationSec == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "영상 스토리는 durationSec가 필요합니다.");
			}
		}

		Story story = new Story();
		story.setUserNo(principal.getUserNo());
		story.setFileId(request.getFileId());
		story.setMediaType(mediaType);
		story.setThumbnailFileId(thumbnailFileId);
		story.setDurationSec(durationSec);
		story.setCaption(blankToNull(request.getCaption()));
		storyMapper.insert(story);
		insertOverlays(story.getStoryId(), principal.getUserNo(), request);
		notificationService.syncStoryTags(story.getStoryId());
		log.info("[create] storyId={} userNo={} mediaType={}",
				story.getStoryId(), principal.getUserNo(), mediaType);
		return toResponse(requireActive(story.getStoryId()), principal.getUserNo());
	}

	@Transactional
	public void delete(UserPrincipal principal, Long storyId) {
		int deleted = storyMapper.softDelete(storyId, principal.getUserNo());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.STORY_NOT_FOUND);
		}
		notificationService.removeByStory(storyId);
		log.info("[delete] storyId={} userNo={}", storyId, principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public List<StoryResponse> listMine(UserPrincipal principal) {
		return storyMapper.findActiveByUserNo(principal.getUserNo()).stream()
				.map(s -> toResponse(s, principal.getUserNo()))
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean hasActiveStory(Long userNo) {
		return storyMapper.countActiveByUserNo(userNo) > 0;
	}

	/** 조회자가 그 사람 스토리를 볼 수 있을 때만 true */
	@Transactional(readOnly = true)
	public boolean hasVisibleStory(Long viewerUserNo, Long ownerUserNo) {
		if (ownerUserNo == null || !hasActiveStory(ownerUserNo)) {
			return false;
		}
		if (viewerUserNo != null && viewerUserNo.equals(ownerUserNo)) {
			return true;
		}
		String audience = privacyService.storyAudience(ownerUserNo);
		if ("PRIVATE".equals(audience)) {
			return false;
		}
		if ("PUBLIC".equals(audience)) {
			return true;
		}
		return viewerUserNo != null && storyMapper.countAcceptedNeighborPair(viewerUserNo, ownerUserNo) > 0;
	}

	/** 조회자 기준. 활성 스토리 중 안 읽은 것이 1개라도 있으면 true. 스토리가 없으면 false */
	@Transactional(readOnly = true)
	public boolean hasUnviewedStory(Long ownerUserNo, Long viewerUserNo) {
		return storyMapper.countUnviewedActiveByOwner(ownerUserNo, viewerUserNo) > 0;
	}

	@Transactional(readOnly = true)
	public List<StoryOwnerResponse> listResidentOwners(UserPrincipal principal) {
		List<StoryOwnerRow> rows = storyMapper.findResidentOwnersWithActiveStories(principal.getUserNo());
		Set<Long> profileIds = new HashSet<>();
		for (StoryOwnerRow row : rows) {
			if (row.getProfileFileId() != null) {
				profileIds.add(row.getProfileFileId().longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(profileIds);
		List<StoryOwnerResponse> result = new ArrayList<>();
		for (StoryOwnerRow row : rows) {
			int unviewed = storyMapper.countUnviewedActiveByOwner(row.getUserNo(), principal.getUserNo());
			Long profileId = row.getProfileFileId() == null ? null : row.getProfileFileId().longValue();
			result.add(StoryOwnerResponse.builder()
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.profileFileId(row.getProfileFileId())
					.profileFile(profileId == null ? null : files.get(profileId))
					.storyCount(row.getStoryCount())
					.hasUnviewed(unviewed > 0)
					.build());
		}
		// 미열람 링 먼저, 같은 그룹은 최신 스토리 순(SQL ORDER) 유지
		result.sort(Comparator.comparing(o -> !Boolean.TRUE.equals(o.getHasUnviewed())));
		return result;
	}

	@Transactional(readOnly = true)
	public List<StoryResponse> listByUser(UserPrincipal principal, Long ownerUserNo) {
		if (ownerUserNo == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "userNo는 필수입니다.");
		}
		assertCanViewOwnerStories(principal.getUserNo(), ownerUserNo);
		return storyMapper.findActiveByUserNo(ownerUserNo).stream()
				.map(s -> toResponse(s, principal.getUserNo()))
				.toList();
	}

	@Transactional
	public StoryResponse markViewed(UserPrincipal principal, Long storyId) {
		Story story = requireActive(storyId);
		assertCanViewOwnerStories(principal.getUserNo(), story.getUserNo());
		storyMapper.insertViewIgnoreDuplicate(storyId, principal.getUserNo());
		log.info("[markViewed] storyId={} viewer={}", storyId, principal.getUserNo());
		return toResponse(story, principal.getUserNo());
	}

	@Transactional
	public StoryLikeResponse like(UserPrincipal principal, Long storyId) {
		Story story = requireActive(storyId);
		assertCanViewOwnerStories(principal.getUserNo(), story.getUserNo());
		StoryLike existing = storyMapper.findLike(storyId, principal.getUserNo());
		boolean changed = false;
		if (existing == null) {
			storyMapper.insertLike(storyId, principal.getUserNo());
			changed = true;
		} else if (Boolean.TRUE.equals(existing.getIsDeleted())) {
			changed = storyMapper.restoreLike(storyId, principal.getUserNo()) > 0;
		}
		if (changed) {
			notificationService.syncStoryLike(storyId, true);
		}
		log.info("[like] storyId={} userNo={}", storyId, principal.getUserNo());
		return toLikeResponse(storyId, principal.getUserNo());
	}

	@Transactional
	public StoryLikeResponse unlike(UserPrincipal principal, Long storyId) {
		Story story = requireActive(storyId);
		assertCanViewOwnerStories(principal.getUserNo(), story.getUserNo());
		if (storyMapper.softDeleteLike(storyId, principal.getUserNo()) > 0) {
			notificationService.syncStoryLike(storyId, false);
		}
		log.info("[unlike] storyId={} userNo={}", storyId, principal.getUserNo());
		return toLikeResponse(storyId, principal.getUserNo());
	}

	private StoryLikeResponse toLikeResponse(Long storyId, Long userNo) {
		return StoryLikeResponse.builder()
				.storyId(storyId)
				.likeCount(storyMapper.countActiveLikes(storyId))
				.likedByMe(storyMapper.countActiveLikeByUser(storyId, userNo) > 0)
				.build();
	}

	/** DM으로 스토리를 공유할 때 보낸 사람이 그 스토리를 볼 수 있는지 */
	public void assertViewable(Long storyId, Long viewerUserNo) {
		Story story = requireActive(storyId);
		assertCanViewOwnerStories(viewerUserNo, story.getUserNo());
	}

	private void assertCanViewOwnerStories(Long viewerUserNo, Long ownerUserNo) {
		if (viewerUserNo.equals(ownerUserNo)) {
			return;
		}
		String audience = privacyService.storyAudience(ownerUserNo);
		if ("PUBLIC".equals(audience)) {
			return;
		}
		if ("PRIVATE".equals(audience)
				|| storyMapper.countAcceptedNeighborPair(viewerUserNo, ownerUserNo) == 0) {
			throw new BusinessException(ErrorCode.STORY_FORBIDDEN);
		}
	}

	private Story requireActive(Long storyId) {
		Story story = storyMapper.findActiveByStoryId(storyId);
		if (story == null) {
			// 만료/삭제 구분 위해 재조회는 생략 — 활성만 노출
			throw new BusinessException(ErrorCode.STORY_NOT_FOUND);
		}
		return story;
	}

	private StoryResponse toResponse(Story story, Long viewerUserNo) {
		User author = userMapper.findActiveByUserNo(story.getUserNo());
		boolean viewedByMe = storyMapper.countView(story.getStoryId(), viewerUserNo) > 0;
		boolean likedByMe = storyMapper.countActiveLikeByUser(story.getStoryId(), viewerUserNo) > 0;
		Set<Long> fileIds = new HashSet<>();
		fileIds.add(story.getFileId());
		if (story.getThumbnailFileId() != null) {
			fileIds.add(story.getThumbnailFileId());
		}
		Integer authorProfileFileId = author == null
				? null
				: petService.representativeProfileFileId(author.getUserNo());
		if (authorProfileFileId != null) {
			fileIds.add(authorProfileFileId.longValue());
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		return StoryResponse.builder()
				.storyId(story.getStoryId())
				.userNo(story.getUserNo())
				.authorNickname(author != null ? author.getNickname() : null)
				.authorProfileFileId(authorProfileFileId)
				.authorProfileFile(authorProfileFileId == null
						? null
						: files.get(authorProfileFileId.longValue()))
				.fileId(story.getFileId())
				.file(files.get(story.getFileId()))
				.mediaType(story.getMediaType())
				.thumbnailFileId(story.getThumbnailFileId())
				.thumbnailFile(story.getThumbnailFileId() == null
						? null
						: files.get(story.getThumbnailFileId()))
				.durationSec(story.getDurationSec())
				.caption(story.getCaption())
				.texts(toTextResponses(storyMapper.findTextsByStoryId(story.getStoryId())))
				.mentions(toMentionResponses(storyMapper.findMentionsByStoryId(story.getStoryId())))
				.createdAt(story.getCreatedAt())
				.expiresAt(story.getExpiresAt())
				.viewedByMe(viewedByMe)
				.likedByMe(likedByMe)
				.build();
	}

	private String resolveMediaType(String requested, FileMeta file) {
		if (StringUtils.hasText(requested)) {
			return requested;
		}
		String mime = file.getMimeType() != null ? file.getMimeType().toLowerCase(Locale.ROOT) : "";
		String ext = file.getExtension() != null ? file.getExtension().toLowerCase(Locale.ROOT) : "";
		if (mime.startsWith("video/") || VIDEO_EXTENSIONS.contains(ext)) {
			return "VIDEO";
		}
		return "IMAGE";
	}

	private void insertOverlays(Long storyId, Long userNo, CreateStoryRequest request) {
		List<CreateStoryRequest.TextItem> texts = request.getTexts() == null ? List.of() : request.getTexts();
		int textOrder = 0;
		for (CreateStoryRequest.TextItem item : texts) {
			int fontSize = item.getFontSize() == null ? 16 : item.getFontSize();
			if (!FONT_SIZES.contains(fontSize)) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "글자 크기는 12, 16, 20, 24만 가능합니다.");
			}
			StoryText row = new StoryText();
			row.setStoryId(storyId);
			row.setContent(item.getContent().trim());
			row.setFontSize(fontSize);
			row.setBold(Boolean.TRUE.equals(item.getBold()));
			row.setUnderline(Boolean.TRUE.equals(item.getUnderline()));
			row.setStrikethrough(Boolean.TRUE.equals(item.getStrikethrough()));
			row.setColor(item.getColor().toUpperCase(Locale.ROOT));
			row.setPosX(item.getPosX());
			row.setPosY(item.getPosY());
			row.setSortOrder(textOrder++);
			storyMapper.insertText(row);
		}

		List<CreateStoryRequest.MentionItem> mentions = request.getMentions() == null ? List.of() : request.getMentions();
		int mentionOrder = 0;
		for (CreateStoryRequest.MentionItem item : mentions) {
			Pet pet = petMapper.findActiveByPetId(item.getPetId());
			if (pet == null) {
				throw new BusinessException(ErrorCode.PET_NOT_FOUND);
			}
			privacyService.assertTagAllowed(userNo, pet.getUserNo());
			StoryMentionRow row = new StoryMentionRow();
			row.setStoryId(storyId);
			row.setPetId(item.getPetId());
			row.setPosX(item.getPosX());
			row.setPosY(item.getPosY());
			row.setSortOrder(mentionOrder++);
			row.setAddedByUserNo(userNo);
			storyMapper.insertMention(row);
		}
	}

	private List<StoryTextResponse> toTextResponses(List<StoryText> rows) {
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<StoryTextResponse> result = new ArrayList<>();
		for (StoryText row : rows) {
			result.add(StoryTextResponse.builder()
					.storyTextId(row.getStoryTextId())
					.content(row.getContent())
					.fontSize(row.getFontSize())
					.bold(Boolean.TRUE.equals(row.getBold()))
					.underline(Boolean.TRUE.equals(row.getUnderline()))
					.strikethrough(Boolean.TRUE.equals(row.getStrikethrough()))
					.color(row.getColor())
					.posX(row.getPosX())
					.posY(row.getPosY())
					.build());
		}
		return result;
	}

	private List<StoryMentionResponse> toMentionResponses(List<StoryMentionRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<StoryMentionResponse> result = new ArrayList<>();
		for (StoryMentionRow row : rows) {
			result.add(StoryMentionResponse.builder()
					.storyMentionId(row.getStoryMentionId())
					.petId(row.getPetId())
					.mentionId(row.getMentionId())
					.petName(row.getPetName())
					.posX(row.getPosX())
					.posY(row.getPosY())
					.build());
		}
		return result;
	}

	private static String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
