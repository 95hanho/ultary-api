package me._hanho.ultary.domain.tag;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.tag.dto.request.ChangeTagHandleRequest;
import me._hanho.ultary.domain.tag.dto.request.CreateTagRequest;
import me._hanho.ultary.domain.tag.dto.request.UpdateTagRequest;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.tag.model.TagImage;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;

	private final TagMapper tagMapper;
	private final FileService fileService;

	@Transactional
	public TagResponse create(UserPrincipal principal, CreateTagRequest request) {
		String handle = blankToNull(request.getHandle());
		if (handle != null) {
			ensureHandleAvailable(handle, null);
		}
		validateImageFiles(request.getImageFileIds());

		Tag tag = new Tag();
		tag.setHashtag(request.getHashtag());
		tag.setTitle(blankToNull(request.getTitle()));
		tag.setHandle(handle);
		tag.setContent(blankToNull(request.getContent()));
		tag.setLink(blankToNull(request.getLink()));
		tag.setCreatedByUserNo(principal.getUserNo());

		tagMapper.insert(tag);
		insertImages(tag.getTagId(), request.getImageFileIds());
		log.info("[create] tagId={} hashtag={} handle={} userNo={}",
				tag.getTagId(), tag.getHashtag(), handle, principal.getUserNo());
		return toResponse(requireActive(tag.getTagId()));
	}

	@Transactional(readOnly = true)
	public TagResponse getDetail(Long tagId) {
		return toResponse(requireActive(tagId));
	}

	@Transactional
	public TagResponse update(UserPrincipal principal, Long tagId, UpdateTagRequest request) {
		requireOwned(tagId, principal.getUserNo());

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
		return toResponse(requireOwned(tagId, principal.getUserNo()));
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

	@Transactional(readOnly = true)
	public List<TagResponse> recommend(String q, Integer limit) {
		String query = StringUtils.hasText(q) ? q.trim() : null;
		return tagMapper.recommend(query, resolveLimit(limit)).stream()
				.map(this::toResponse)
				.toList();
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
			return toResponse(tag);
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
		return toResponse(requireOwned(tagId, principal.getUserNo()));
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
		List<Long> imageFileIds = tagMapper.findImageFileIdsByTagId(tag.getTagId());
		return TagResponse.builder()
				.tagId(tag.getTagId())
				.hashtag(tag.getHashtag())
				.title(tag.getTitle())
				.handle(tag.getHandle())
				.handleChangedAt(tag.getHandleChangedAt())
				.content(tag.getContent())
				.link(tag.getLink())
				.useCount(tag.getUseCount())
				.createdByUserNo(tag.getCreatedByUserNo())
				.imageFileIds(imageFileIds != null ? imageFileIds : List.of())
				.createdAt(tag.getCreatedAt())
				.updatedAt(tag.getUpdatedAt())
				.build();
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
