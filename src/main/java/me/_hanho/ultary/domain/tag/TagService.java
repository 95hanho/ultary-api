package me._hanho.ultary.domain.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.tag.dto.request.ChangeTagHandleRequest;
import me._hanho.ultary.domain.tag.dto.request.CreateTagRequest;
import me._hanho.ultary.domain.tag.dto.request.SubmitTagProfileRequest;
import me._hanho.ultary.domain.tag.dto.request.UpdateTagRequest;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestPageResponse;
import me._hanho.ultary.domain.tag.dto.response.TagProfileRequestResponse;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.tag.model.TagProfileRequest;
import me._hanho.ultary.domain.tag.model.TagSearchHit;
import me._hanho.ultary.domain.tag.model.TagImage;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;
	/** 게시글 내용 기반 추천 상한 */
	private static final int RECOMMEND_MAX = 5;
	private static final int REQUEST_DEFAULT_LIMIT = 30;
	private static final Set<String> REQUEST_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

	private final TagMapper tagMapper;
	private final FileService fileService;
	private final AiService aiService;

	@Transactional
	public TagResponse create(UserPrincipal principal, CreateTagRequest request) {
		String handle = blankToNull(request.getHandle());
		if (handle != null) {
			ensureHandleAvailable(handle, null);
		}
		String title = blankToNull(request.getTitle());
		String content = blankToNull(request.getContent());
		String link = blankToNull(request.getLink());
		List<Long> imageFileIds = cleanIds(request.getImageFileIds());
		boolean commercial = title != null || content != null || link != null || !imageFileIds.isEmpty();
		if (commercial) {
			validateImageFiles(imageFileIds);
		}

		Tag tag = new Tag();
		tag.setHashtag(request.getHashtag());
		tag.setPurpose(commercial ? "COMMERCIAL" : "USER");
		tag.setProfileStatus(commercial ? "PENDING" : "NONE");
		tag.setHandle(handle);
		tag.setCreatedByUserNo(principal.getUserNo());

		tagMapper.insert(tag);
		if (commercial) {
			saveProfileRequest(tag.getTagId(), principal.getUserNo(), title, content, link, imageFileIds, null);
			tagMapper.markCommercialRequested(tag.getTagId());
		}
		log.info("[create] tagId={} hashtag={} purpose={} userNo={}",
				tag.getTagId(), tag.getHashtag(), tag.getPurpose(), principal.getUserNo());
		return toResponse(requireActive(tag.getTagId()), null, principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public TagResponse getDetail(Long tagId, Long viewerUserNo) {
		Tag tag = requireActive(tagId);
		return toResponse(tag, tagMapper.countActiveFeeds(tag.getTagId()), viewerUserNo);
	}

	@Transactional
	public TagResponse update(UserPrincipal principal, Long tagId, UpdateTagRequest request) {
		Tag owned = requireOwned(tagId, principal.getUserNo());
		boolean submitting = request.getTitle() != null || request.getContent() != null || request.getLink() != null;
		if (submitting) {
			SubmitTagProfileRequest body = new SubmitTagProfileRequest();
			body.setTitle(request.getTitle() != null
					? request.getTitle()
					: (Boolean.TRUE.equals(request.getClearTitle()) ? "" : owned.getTitle()));
			body.setContent(request.getContent() != null
					? request.getContent()
					: (Boolean.TRUE.equals(request.getClearContent()) ? "" : owned.getContent()));
			body.setLink(request.getLink() != null
					? request.getLink()
					: (Boolean.TRUE.equals(request.getClearLink()) ? "" : owned.getLink()));
			body.setImageFileIds(tagMapper.findImageFileIdsByTagId(tagId));
			submitProfile(principal, tagId, body);
			return toResponse(requireActive(tagId), null, principal.getUserNo());
		}

		Tag patch = new Tag();
		patch.setTagId(tagId);
		patch.setCreatedByUserNo(principal.getUserNo());
		patch.setClearTitle(Boolean.TRUE.equals(request.getClearTitle()));
		patch.setClearContent(Boolean.TRUE.equals(request.getClearContent()));
		patch.setClearLink(Boolean.TRUE.equals(request.getClearLink()));
		if (!Boolean.TRUE.equals(request.getClearTitle()) && request.getTitle() != null) {
			patch.setTitle(request.getTitle().trim());
		}
		if (!Boolean.TRUE.equals(request.getClearContent()) && request.getContent() != null) {
			patch.setContent(request.getContent().trim());
		}
		if (!Boolean.TRUE.equals(request.getClearLink()) && request.getLink() != null) {
			patch.setLink(request.getLink().trim());
		}

		int updated = tagMapper.update(patch);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}
		log.info("[update] tagId={} userNo={}", tagId, principal.getUserNo());
		return toResponse(requireOwned(tagId, principal.getUserNo()), null, principal.getUserNo());
	}

	/** 상업 태그에 보일 제목·소개·링크·이미지. 승인 전에는 태그 조회에 안 나온다. */
	@Transactional
	public TagProfileRequestResponse submitProfile(UserPrincipal principal, Long tagId, SubmitTagProfileRequest request) {
		requireActive(tagId);
		String title = blankToNull(request.getTitle());
		String content = blankToNull(request.getContent());
		String link = blankToNull(request.getLink());
		List<Long> imageFileIds = cleanIds(request.getImageFileIds());
		if (title == null && content == null && link == null && imageFileIds.isEmpty()) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_EMPTY);
		}
		validateImageFiles(imageFileIds);
		TagProfileRequest saved = saveProfileRequest(
				tagId, principal.getUserNo(), title, content, link, imageFileIds,
				tagMapper.findPendingByTagId(tagId));
		tagMapper.markCommercialRequested(tagId);
		log.info("[submitProfile] tagId={} requestId={} userNo={}",
				tagId, saved.getTagProfileRequestId(), principal.getUserNo());
		return toRequestResponse(tagMapper.findPendingByTagId(tagId));
	}

	@Transactional(readOnly = true)
	public List<TagProfileRequestResponse> listMyProfileRequests(Long userNo, String status, int limit) {
		return tagMapper.findProfileRequests(
						userNo, normalizeRequestStatus(status), null, resolveRequestLimit(limit))
				.stream()
				.map(this::toRequestResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public int countPendingProfileRequests() {
		return tagMapper.countProfileRequests("PENDING");
	}

	@Transactional(readOnly = true)
	public TagProfileRequestPageResponse listProfileRequestsForAdmin(String status, Long cursorRequestId, int limit) {
		String normalized = normalizeRequestStatus(status);
		int size = resolveRequestLimit(limit);
		List<TagProfileRequest> rows = tagMapper.findProfileRequests(
				null, normalized, cursorRequestId, size + 1);
		Long nextCursorRequestId = null;
		if (rows.size() > size) {
			rows = rows.subList(0, size);
			nextCursorRequestId = rows.get(rows.size() - 1).getTagProfileRequestId();
		}
		return TagProfileRequestPageResponse.builder()
				.items(rows.stream().map(this::toRequestResponse).toList())
				.nextCursorRequestId(nextCursorRequestId)
				.totalCount(tagMapper.countProfileRequests(normalized))
				.build();
	}

	/** 최신 요청을 승인한다. 대기·승인·거절 모두 다시 승인할 수 있다. */
	@Transactional
	public TagProfileRequestResponse approveProfile(Long adminNo, Long tagId) {
		TagProfileRequest latest = requireLatest(tagId);
		if (tagMapper.reviewProfileRequest(latest.getTagProfileRequestId(), "APPROVED", adminNo, null) == 0) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_NOT_FOUND);
		}
		tagMapper.applyApprovedProfile(latest);
		tagMapper.deleteImagesByTagId(tagId);
		insertImages(tagId, tagMapper.findRequestImageFileIds(latest.getTagProfileRequestId()));
		log.info("[approveProfile] tagId={} requestId={} adminNo={}",
				tagId, latest.getTagProfileRequestId(), adminNo);
		return toRequestResponse(reloadRequest(latest.getTagProfileRequestId()));
	}

	/** 최신 요청을 거절한다. 이미 공개된 설명은 그대로 둔다. */
	@Transactional
	public TagProfileRequestResponse rejectProfile(Long adminNo, Long tagId, String rejectMessage) {
		TagProfileRequest latest = requireLatest(tagId);
		String message = blankToNull(rejectMessage);
		if (tagMapper.reviewProfileRequest(latest.getTagProfileRequestId(), "REJECTED", adminNo, message) == 0) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_NOT_FOUND);
		}
		tagMapper.markProfileRejected(tagId);
		log.info("[rejectProfile] tagId={} requestId={} adminNo={}",
				tagId, latest.getTagProfileRequestId(), adminNo);
		return toRequestResponse(reloadRequest(latest.getTagProfileRequestId()));
	}

	/**
	 * 최신 요청을 대기로 되돌린다.
	 * 그 요청이 승인으로 공개 중이면 설명을 숨긴다.
	 * 거절을 대기로 되돌릴 때 이전에 공개된 설명은 그대로 둔다.
	 */
	@Transactional
	public TagProfileRequestResponse reopenProfile(Long tagId) {
		TagProfileRequest latest = requireLatest(tagId);
		boolean wasApproved = "APPROVED".equals(latest.getStatus());
		if (tagMapper.reopenProfileRequest(latest.getTagProfileRequestId()) == 0) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_NOT_FOUND);
		}
		if (wasApproved) {
			tagMapper.hidePublishedProfile(tagId);
		}
		log.info("[reopenProfile] tagId={} requestId={} wasApproved={}",
				tagId, latest.getTagProfileRequestId(), wasApproved);
		return toRequestResponse(reloadRequest(latest.getTagProfileRequestId()));
	}

	@Transactional(readOnly = true)
	public List<TagResponse> search(String q, Integer limit) {
		if (!StringUtils.hasText(q)) {
			return List.of();
		}
		return tagMapper.search(q.trim(), resolveLimit(limit)).stream()
				.map(this::toResponse)
				.toList();
	}

	/** 검색창 태그명. 해시태그에 포함되고, feedCount는 조회자에게 보이는 글 수 */
	@Transactional(readOnly = true)
	public List<TagSearchHit> searchByHashtag(Long viewerUserNo, String q, int limit) {
		return tagMapper.searchByHashtag(viewerUserNo, q, limit);
	}

	/**
	 * 게시글 작성 중 내용(q) 기반 태그 추천.
	 * Gemini 우선, 실패·비활성·빈 결과면 기존 DB LIKE 추천으로 fallback.
	 * 최대 5개. 기존 DB 태그가 있으면 tagId 등을 채운다.
	 */
	@Transactional(readOnly = true)
	public List<TagResponse> recommend(UserPrincipal principal, String q, Integer limit) {
		int capped = Math.min(resolveLimit(limit), RECOMMEND_MAX);
		String query = StringUtils.hasText(q) ? q.trim() : null;
		Long userNo = principal != null ? principal.getUserNo() : null;

		if (query != null) {
			List<String> aiTags = aiService.recommendHashtags(userNo, query);
			if (!aiTags.isEmpty()) {
				return toRecommendResponses(aiTags, capped);
			}
		}
		return recommendFromDb(query, capped);
	}

	private List<TagResponse> recommendFromDb(String query, int limit) {
		return tagMapper.recommend(query, limit).stream()
				.map(this::toResponse)
				.toList();
	}

	private List<TagResponse> toRecommendResponses(List<String> hashtags, int limit) {
		List<TagResponse> result = new ArrayList<>();
		for (String hashtag : hashtags) {
			if (result.size() >= limit) {
				break;
			}
			Tag existing = tagMapper.findActiveByHashtag(hashtag);
			if (existing != null) {
				result.add(toResponse(existing));
			} else {
				result.add(TagResponse.builder()
						.tagId(null)
						.hashtag(hashtag)
						.purpose("USER")
						.profileStatus("NONE")
						.useCount(0)
						.imageFileIds(List.of())
						.images(List.of())
						.build());
			}
		}
		return result;
	}

	@Transactional(readOnly = true)
	public ChangeAvailabilityResponse handleChangeAvailability(UserPrincipal principal, Long tagId) {
		Tag tag = requireOwned(tagId, principal.getUserNo());
		return IdentityChangeCooldown.availability(
				tag.getHandleChangedAt(),
				IdentityChangeCooldown.HANDLE_DAYS);
	}

	@Transactional
	public TagResponse changeHandle(UserPrincipal principal, Long tagId, ChangeTagHandleRequest request) {
		Tag tag = requireOwned(tagId, principal.getUserNo());
		String handle = request.getHandle().trim();
		if (!StringUtils.hasText(handle)) {
			throw new BusinessException(ErrorCode.TAG_HANDLE_REQUIRED);
		}
		if (handle.equalsIgnoreCase(tag.getHandle() != null ? tag.getHandle() : "")) {
			return toResponse(tag, null, principal.getUserNo());
		}

		IdentityChangeCooldown.requireChangeable(
				tag.getHandleChangedAt(),
				IdentityChangeCooldown.HANDLE_DAYS,
				ErrorCode.TAG_HANDLE_CHANGE_COOLDOWN);

		ensureHandleAvailable(handle, tagId);

		int updated = tagMapper.updateHandle(tagId, principal.getUserNo(), handle);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}
		log.info("[changeHandle] tagId={} handle={}", tagId, handle);
		return toResponse(requireOwned(tagId, principal.getUserNo()), null, principal.getUserNo());
	}

	private TagProfileRequest saveProfileRequest(
			Long tagId,
			Long userNo,
			String title,
			String content,
			String link,
			List<Long> imageFileIds,
			TagProfileRequest pending) {
		if (pending != null && !userNo.equals(pending.getRequesterUserNo())) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_OPEN);
		}
		if (pending != null) {
			pending.setTitle(title);
			pending.setContent(content);
			pending.setLink(link);
			tagMapper.updatePendingProfileRequest(pending);
			tagMapper.deleteRequestImages(pending.getTagProfileRequestId());
			insertRequestImages(pending.getTagProfileRequestId(), imageFileIds);
			return pending;
		}
		TagProfileRequest created = new TagProfileRequest();
		created.setTagId(tagId);
		created.setRequesterUserNo(userNo);
		created.setTitle(title);
		created.setContent(content);
		created.setLink(link);
		tagMapper.insertProfileRequest(created);
		insertRequestImages(created.getTagProfileRequestId(), imageFileIds);
		return created;
	}

	private void insertRequestImages(Long requestId, List<Long> imageFileIds) {
		int order = 0;
		for (Long fileId : imageFileIds) {
			tagMapper.insertRequestImage(requestId, fileId, order++);
		}
	}

	private TagProfileRequest requireLatest(Long tagId) {
		requireActive(tagId);
		TagProfileRequest latest = tagMapper.findLatestByTagId(tagId);
		if (latest == null) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_NOT_FOUND);
		}
		return latest;
	}

	private TagProfileRequest reloadRequest(Long requestId) {
		TagProfileRequest row = tagMapper.findProfileRequestById(requestId);
		if (row == null) {
			throw new BusinessException(ErrorCode.TAG_PROFILE_REQUEST_NOT_FOUND);
		}
		return row;
	}

	private String normalizeRequestStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		String normalized = status.trim().toUpperCase();
		if (!REQUEST_STATUSES.contains(normalized)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "태그 설명 요청 상태가 올바르지 않습니다.");
		}
		return normalized;
	}

	private int resolveRequestLimit(int limit) {
		if (limit < 1) {
			return REQUEST_DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private List<Long> cleanIds(List<Long> imageFileIds) {
		if (imageFileIds == null || imageFileIds.isEmpty()) {
			return List.of();
		}
		return imageFileIds.stream().filter(id -> id != null).toList();
	}

	private void insertImages(Long tagId, List<Long> imageFileIds) {
		if (imageFileIds == null || imageFileIds.isEmpty()) {
			return;
		}
		int order = 0;
		for (Long fileId : imageFileIds) {
			if (fileId == null) {
				continue;
			}
			TagImage image = new TagImage();
			image.setTagId(tagId);
			image.setFileId(fileId);
			image.setSortOrder(order++);
			tagMapper.insertImage(image);
		}
	}

	private void validateImageFiles(List<Long> imageFileIds) {
		if (imageFileIds == null) {
			return;
		}
		for (Long fileId : imageFileIds) {
			if (fileId != null) {
				fileService.requireActive(fileId);
			}
		}
	}

	private void ensureHandleAvailable(String handle, Long excludeTagId) {
		if (tagMapper.countByHandle(handle, excludeTagId) > 0) {
			throw new BusinessException(ErrorCode.TAG_HANDLE_DUPLICATED);
		}
	}

	public void ensureActive(Long tagId) {
		requireActive(tagId);
	}

	private Tag requireActive(Long tagId) {
		Tag tag = tagMapper.findActiveByTagId(tagId);
		if (tag == null) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}
		return tag;
	}

	private Tag requireOwned(Long tagId, Long userNo) {
		Tag tag = requireActive(tagId);
		if (tag.getCreatedByUserNo() == null || !tag.getCreatedByUserNo().equals(userNo)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return tag;
	}

	private TagResponse toResponse(Tag tag) {
		return toResponse(tag, null, null);
	}

	private TagResponse toResponse(Tag tag, Integer feedCount, Long viewerUserNo) {
		boolean published = "APPROVED".equals(tag.getProfileStatus());
		List<Long> ids = List.of();
		List<FileSummaryResponse> images = List.of();
		if (published) {
			List<Long> imageFileIds = tagMapper.findImageFileIdsByTagId(tag.getTagId());
			ids = imageFileIds != null ? imageFileIds : List.of();
			images = summaries(ids);
		}
		TagProfileRequestResponse mine = null;
		if (viewerUserNo != null) {
			TagProfileRequest latest = tagMapper.findLatestByTagAndRequester(tag.getTagId(), viewerUserNo);
			if (latest != null) {
				mine = toRequestResponse(latest);
			}
		}
		return TagResponse.builder()
				.tagId(tag.getTagId())
				.hashtag(tag.getHashtag())
				.purpose(tag.getPurpose())
				.profileStatus(tag.getProfileStatus())
				.title(published ? tag.getTitle() : null)
				.handle(tag.getHandle())
				.handleChangedAt(tag.getHandleChangedAt())
				.content(published ? tag.getContent() : null)
				.link(published ? tag.getLink() : null)
				.useCount(tag.getUseCount())
				.feedCount(feedCount)
				.createdByUserNo(tag.getCreatedByUserNo())
				.imageFileIds(ids)
				.images(images)
				.profileRequest(mine)
				.createdAt(tag.getCreatedAt())
				.updatedAt(tag.getUpdatedAt())
				.build();
	}

	private TagProfileRequestResponse toRequestResponse(TagProfileRequest row) {
		List<Long> ids = tagMapper.findRequestImageFileIds(row.getTagProfileRequestId());
		if (ids == null) {
			ids = List.of();
		}
		return TagProfileRequestResponse.builder()
				.requestId(row.getTagProfileRequestId())
				.tagId(row.getTagId())
				.hashtag(row.getHashtag())
				.requesterUserNo(row.getRequesterUserNo())
				.requesterNickname(row.getRequesterNickname())
				.title(row.getTitle())
				.content(row.getContent())
				.link(row.getLink())
				.imageFileIds(ids)
				.images(summaries(ids))
				.status(row.getStatus())
				.rejectMessage(row.getRejectMessage())
				.createdAt(row.getCreatedAt())
				.reviewedAt(row.getReviewedAt())
				.build();
	}

	private List<FileSummaryResponse> summaries(List<Long> ids) {
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(ids);
		return ids.stream().map(files::get).filter(file -> file != null).toList();
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private static String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
