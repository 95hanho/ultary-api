package me._hanho.ultary.domain.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.notification.dto.request.UpdateNotificationSettingRequest;
import me._hanho.ultary.domain.notification.dto.response.NotificationSettingResponse;
import me._hanho.ultary.domain.notification.model.NotificationSetting;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

	private final NotificationSettingMapper notificationSettingMapper;

	@Transactional(readOnly = true)
	public NotificationSettingResponse get(UserPrincipal principal) {
		return toResponse(notificationSettingMapper.findByUserNo(principal.getUserNo()));
	}

	@Transactional
	public NotificationSettingResponse update(UserPrincipal principal, UpdateNotificationSettingRequest request) {
		if (request == null || !request.hasAny()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "변경할 알림 설정이 없습니다.");
		}
		NotificationSetting current = notificationSettingMapper.findByUserNo(principal.getUserNo());
		if (current == null) {
			current = defaults(principal.getUserNo());
		}
		if (request.getNeighbor() != null) {
			current.setNeighbor(request.getNeighbor());
		}
		if (request.getLikePost() != null) {
			current.setLikePost(request.getLikePost());
		}
		if (request.getLikeComment() != null) {
			current.setLikeComment(request.getLikeComment());
		}
		if (request.getLikeReply() != null) {
			current.setLikeReply(request.getLikeReply());
		}
		if (request.getCommentOnPost() != null) {
			current.setCommentOnPost(request.getCommentOnPost());
		}
		if (request.getReplyOnComment() != null) {
			current.setReplyOnComment(request.getReplyOnComment());
		}
		if (request.getMention() != null) {
			current.setMention(request.getMention());
		}
		if (request.getTagPost() != null) {
			current.setTagPost(request.getTagPost());
		}
		if (request.getTagStory() != null) {
			current.setTagStory(request.getTagStory());
		}
		if (request.getStoryReact() != null) {
			current.setStoryReact(request.getStoryReact());
		}
		notificationSettingMapper.upsert(current);
		return toResponse(current);
	}

	/** 행이 없거나 켜져 있으면 true. 댓글·답글 통합은 켜진 쪽이 하나라도 있을 때 */
	@Transactional(readOnly = true)
	public boolean allows(Long userNo, String type, int hasComment, int hasReply) {
		NotificationSetting setting = notificationSettingMapper.findByUserNo(userNo);
		if (setting == null || type == null) {
			return true;
		}
		return switch (type) {
			case NotificationService.NEIGHBOR_REQUEST -> on(setting.getNeighbor());
			case NotificationService.FEED_LIKE -> on(setting.getLikePost());
			case NotificationService.COMMENT_LIKE -> on(setting.getLikeComment());
			case NotificationService.REPLY_LIKE -> on(setting.getLikeReply());
			case NotificationService.FEED_COMMENT ->
					(hasComment > 0 && on(setting.getCommentOnPost()))
							|| (hasReply > 0 && on(setting.getReplyOnComment()));
			case NotificationService.COMMENT_MENTION, NotificationService.REPLY_MENTION -> on(setting.getMention());
			case NotificationService.FEED_TAG -> on(setting.getTagPost());
			case NotificationService.STORY_TAG -> on(setting.getTagStory());
			case NotificationService.STORY_LIKE -> on(setting.getStoryReact());
			default -> true;
		};
	}

	private NotificationSetting defaults(Long userNo) {
		NotificationSetting setting = new NotificationSetting();
		setting.setUserNo(userNo);
		setting.setNeighbor(true);
		setting.setLikePost(true);
		setting.setLikeComment(true);
		setting.setLikeReply(true);
		setting.setCommentOnPost(true);
		setting.setReplyOnComment(true);
		setting.setMention(true);
		setting.setTagPost(true);
		setting.setTagStory(true);
		setting.setStoryReact(true);
		return setting;
	}

	private NotificationSettingResponse toResponse(NotificationSetting setting) {
		if (setting == null) {
			setting = defaults(null);
		}
		return NotificationSettingResponse.builder()
				.neighbor(on(setting.getNeighbor()))
				.likePost(on(setting.getLikePost()))
				.likeComment(on(setting.getLikeComment()))
				.likeReply(on(setting.getLikeReply()))
				.commentOnPost(on(setting.getCommentOnPost()))
				.replyOnComment(on(setting.getReplyOnComment()))
				.mention(on(setting.getMention()))
				.tagPost(on(setting.getTagPost()))
				.tagStory(on(setting.getTagStory()))
				.storyReact(on(setting.getStoryReact()))
				.build();
	}

	private static boolean on(Boolean value) {
		return !Boolean.FALSE.equals(value);
	}
}
