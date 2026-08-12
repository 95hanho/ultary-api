package me._hanho.ultary.domain.tag;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.exception.NotImplemented;
import me._hanho.ultary.common.response.ChangeAvailabilityResponse;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.tag.dto.request.ChangeTagHandleRequest;
import me._hanho.ultary.domain.tag.dto.response.TagResponse;
import me._hanho.ultary.domain.tag.TagMapper;
import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

	private final TagMapper tagMapper;

	public void create() {
		NotImplemented.yet();
	}

	public void getDetail(Long tagId) {
		NotImplemented.yet();
	}

	public void search(String q) {
		NotImplemented.yet();
	}

	public void recommend(String q) {
		NotImplemented.yet();
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

		if (tagMapper.countByHandle(handle, tagId) > 0) {
			throw new BusinessException(ErrorCode.TAG_HANDLE_DUPLICATED);
		}

		int updated = tagMapper.updateHandle(tagId, principal.getUserNo(), handle);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}
		log.info("[changeHandle] tagId={} handle={}", tagId, handle);
		return toResponse(requireOwned(tagId, principal.getUserNo()));
	}

	private Tag requireOwned(Long tagId, Long userNo) {
		Tag tag = tagMapper.findActiveByTagId(tagId);
		if (tag == null) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}
		if (tag.getCreatedByUserNo() == null || !tag.getCreatedByUserNo().equals(userNo)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return tag;
	}

	private TagResponse toResponse(Tag tag) {
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
				.createdAt(tag.getCreatedAt())
				.updatedAt(tag.getUpdatedAt())
				.build();
	}
}
