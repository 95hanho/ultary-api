package me._hanho.ultary.domain.settings;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.domain.settings.dto.response.PrivacyResponse;
import me._hanho.ultary.domain.settings.model.UserPrivacy;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class PrivacyService {

	static final String PUBLIC = "PUBLIC";
	static final String NEIGHBORS = "NEIGHBORS";
	static final String PRIVATE = "PRIVATE";
	private static final Set<String> VISIBILITIES = Set.of(PUBLIC, NEIGHBORS, PRIVATE);

	private final PrivacyMapper privacyMapper;

	@Transactional(readOnly = true)
	public PrivacyResponse get(UserPrincipal principal) {
		return toResponse(load(principal.getUserNo()));
	}

	@Transactional
	public PrivacyResponse update(UserPrincipal principal, UpdatePrivacyRequest request) {
		if (request == null || !request.hasAny()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "변경할 공개 범위가 없습니다.");
		}
		UserPrivacy current = load(principal.getUserNo());
		if (request.getPrivateAccount() != null) {
			current.setPrivateAccount(request.getPrivateAccount());
		}
		if (request.getFeedVisibility() != null) {
			current.setFeedVisibility(normalizeVisibility(request.getFeedVisibility()));
		}
		if (request.getStoryVisibility() != null) {
			current.setStoryVisibility(normalizeVisibility(request.getStoryVisibility()));
		}
		if (request.getNeighborRequest() != null) {
			current.setNeighborRequest(request.getNeighborRequest());
		}
		if (request.getAllowComment() != null) {
			current.setAllowComment(request.getAllowComment());
		}
		if (request.getAllowMention() != null) {
			current.setAllowMention(request.getAllowMention());
		}
		if (request.getAllowTag() != null) {
			current.setAllowTag(request.getAllowTag());
		}
		privacyMapper.upsert(current);
		return toResponse(current);
	}

	/** 요청에 공개 범위가 없으면 계정의 게시글 기본값 */
	@Transactional(readOnly = true)
	public String resolveFeedVisibility(Long userNo, String requested) {
		if (!StringUtils.hasText(requested)) {
			return load(userNo).getFeedVisibility();
		}
		return normalizeVisibility(requested);
	}

	/** 비공개 계정이면 PUBLIC 게시글도 이웃만 */
	@Transactional(readOnly = true)
	public String feedAudience(Long ownerUserNo, String feedVisibility) {
		if (PRIVATE.equals(feedVisibility)) {
			return PRIVATE;
		}
		if (NEIGHBORS.equals(feedVisibility) || isPrivateAccount(ownerUserNo)) {
			return NEIGHBORS;
		}
		return PUBLIC;
	}

	/** 비공개 계정이면 PUBLIC 스토리도 이웃만. 행이 없으면 NEIGHBORS */
	@Transactional(readOnly = true)
	public String storyAudience(Long ownerUserNo) {
		UserPrivacy privacy = load(ownerUserNo);
		if (PRIVATE.equals(privacy.getStoryVisibility())) {
			return PRIVATE;
		}
		if (NEIGHBORS.equals(privacy.getStoryVisibility()) || Boolean.TRUE.equals(privacy.getPrivateAccount())) {
			return NEIGHBORS;
		}
		return PUBLIC;
	}

	@Transactional(readOnly = true)
	public boolean isPrivateAccount(Long userNo) {
		return Boolean.TRUE.equals(load(userNo).getPrivateAccount());
	}

	@Transactional(readOnly = true)
	public void assertNeighborRequestOpen(Long targetUserNo) {
		if (!Boolean.TRUE.equals(load(targetUserNo).getNeighborRequest())) {
			throw new BusinessException(ErrorCode.NEIGHBOR_REQUEST_CLOSED);
		}
	}

	@Transactional(readOnly = true)
	public void assertCommentAllowed(Long actorUserNo, Long ownerUserNo) {
		if (actorUserNo != null && actorUserNo.equals(ownerUserNo)) {
			return;
		}
		if (!Boolean.TRUE.equals(load(ownerUserNo).getAllowComment())) {
			throw new BusinessException(ErrorCode.COMMENT_NOT_ALLOWED);
		}
	}

	@Transactional(readOnly = true)
	public void assertMentionAllowed(Long actorUserNo, Long targetUserNo) {
		if (targetUserNo == null || targetUserNo.equals(actorUserNo)) {
			return;
		}
		if (!Boolean.TRUE.equals(load(targetUserNo).getAllowMention())) {
			throw new BusinessException(ErrorCode.MENTION_NOT_ALLOWED);
		}
	}

	@Transactional(readOnly = true)
	public void assertTagAllowed(Long actorUserNo, Long ownerUserNo) {
		if (ownerUserNo == null || ownerUserNo.equals(actorUserNo)) {
			return;
		}
		if (!Boolean.TRUE.equals(load(ownerUserNo).getAllowTag())) {
			throw new BusinessException(ErrorCode.TAG_NOT_ALLOWED);
		}
	}

	private UserPrivacy load(Long userNo) {
		UserPrivacy privacy = privacyMapper.findByUserNo(userNo);
		if (privacy != null) {
			return privacy;
		}
		UserPrivacy defaults = new UserPrivacy();
		defaults.setUserNo(userNo);
		defaults.setPrivateAccount(false);
		defaults.setFeedVisibility(PUBLIC);
		defaults.setStoryVisibility(NEIGHBORS);
		defaults.setNeighborRequest(true);
		defaults.setAllowComment(true);
		defaults.setAllowMention(true);
		defaults.setAllowTag(true);
		return defaults;
	}

	private PrivacyResponse toResponse(UserPrivacy privacy) {
		return PrivacyResponse.builder()
				.privateAccount(Boolean.TRUE.equals(privacy.getPrivateAccount()))
				.feedVisibility(privacy.getFeedVisibility())
				.storyVisibility(privacy.getStoryVisibility())
				.neighborRequest(!Boolean.FALSE.equals(privacy.getNeighborRequest()))
				.allowComment(!Boolean.FALSE.equals(privacy.getAllowComment()))
				.allowMention(!Boolean.FALSE.equals(privacy.getAllowMention()))
				.allowTag(!Boolean.FALSE.equals(privacy.getAllowTag()))
				.build();
	}

	private String normalizeVisibility(String visibility) {
		if (!StringUtils.hasText(visibility)) {
			throw new BusinessException(ErrorCode.FEED_INVALID_VISIBILITY);
		}
		String value = visibility.trim().toUpperCase(Locale.ROOT);
		if (!VISIBILITIES.contains(value)) {
			throw new BusinessException(ErrorCode.FEED_INVALID_VISIBILITY);
		}
		return value;
	}
}
