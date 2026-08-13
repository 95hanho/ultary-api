package me._hanho.ultary.domain.story;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.model.FileMeta;
import me._hanho.ultary.domain.story.dto.request.CreateStoryRequest;
import me._hanho.ultary.domain.story.dto.response.StoryOwnerResponse;
import me._hanho.ultary.domain.story.dto.response.StoryResponse;
import me._hanho.ultary.domain.story.model.Story;
import me._hanho.ultary.domain.story.model.StoryOwnerRow;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

	private final StoryMapper storyMapper;
	private final FileService fileService;
	private final UserMapper userMapper;

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

	@Transactional(readOnly = true)
	public List<StoryOwnerResponse> listResidentOwners(UserPrincipal principal) {
		List<StoryOwnerRow> rows = storyMapper.findResidentOwnersWithActiveStories(principal.getUserNo());
		List<StoryOwnerResponse> result = new ArrayList<>();
		for (StoryOwnerRow row : rows) {
			int unviewed = storyMapper.countUnviewedActiveByOwner(row.getUserNo(), principal.getUserNo());
			result.add(StoryOwnerResponse.builder()
					.userNo(row.getUserNo())
					.nickname(row.getNickname())
					.profileFileId(row.getProfileFileId())
					.storyCount(row.getStoryCount())
					.hasUnviewed(unviewed > 0)
					.build());
		}
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
		if (!story.getUserNo().equals(principal.getUserNo())) {
			storyMapper.insertViewIgnoreDuplicate(storyId, principal.getUserNo());
		}
		log.info("[markViewed] storyId={} viewer={}", storyId, principal.getUserNo());
		return toResponse(story, principal.getUserNo());
	}

	private void assertCanViewOwnerStories(Long viewerUserNo, Long ownerUserNo) {
		if (viewerUserNo.equals(ownerUserNo)) {
			return;
		}
		if (storyMapper.countAcceptedNeighborPair(viewerUserNo, ownerUserNo) == 0) {
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
		boolean viewedByMe = story.getUserNo().equals(viewerUserNo)
				|| storyMapper.countView(story.getStoryId(), viewerUserNo) > 0;
		return StoryResponse.builder()
				.storyId(story.getStoryId())
				.userNo(story.getUserNo())
				.authorNickname(author != null ? author.getNickname() : null)
				.authorProfileFileId(author != null ? author.getProfileFileId() : null)
				.fileId(story.getFileId())
				.mediaType(story.getMediaType())
				.thumbnailFileId(story.getThumbnailFileId())
				.durationSec(story.getDurationSec())
				.caption(story.getCaption())
				.createdAt(story.getCreatedAt())
				.expiresAt(story.getExpiresAt())
				.viewedByMe(viewedByMe)
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

	private static String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
