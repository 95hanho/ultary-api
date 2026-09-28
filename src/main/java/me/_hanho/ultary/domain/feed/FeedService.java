package me._hanho.ultary.domain.feed;

import java.util.ArrayList;
import java.util.HashMap;
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
import me._hanho.ultary.domain.neighbor.NeighborService;
import me._hanho.ultary.domain.feed.dto.request.CommentMentionRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateReplyRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateReplyRequest;
import me._hanho.ultary.domain.feed.dto.response.CommentMentionResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedCommentResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedLikerResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedReplyResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedShareResponse;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.feed.model.FeedComment;
import me._hanho.ultary.domain.feed.model.FeedCommentLike;
import me._hanho.ultary.domain.feed.model.FeedCommentMention;
import me._hanho.ultary.domain.feed.model.FeedLike;
import me._hanho.ultary.domain.feed.model.FeedMedia;
import me._hanho.ultary.domain.feed.model.FeedMediaMention;
import me._hanho.ultary.domain.feed.model.FeedPet;
import me._hanho.ultary.domain.feed.model.FeedReply;
import me._hanho.ultary.domain.feed.model.FeedReplyLike;
import me._hanho.ultary.domain.feed.model.FeedStore;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.file.model.FileMeta;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.model.Pet;
import me._hanho.ultary.domain.tag.TagMapper;
import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;
import me._hanho.ultary.security.principal.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

	private static final int DEFAULT_LIST_LIMIT = 50;
	private static final int MAX_LIST_LIMIT = 100;
	/** 게시글 댓글+답글 합이 이 값 이하면 댓글 목록에 답글을 같이 내려줌 */
	private static final int INLINE_REPLY_THRESHOLD = 10;
	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

	private final FeedMapper feedMapper;
	private final FileService fileService;
	private final PetMapper petMapper;
	private final TagMapper tagMapper;
	private final UserMapper userMapper;
	private final NeighborService neighborService;

	@Transactional
	public FeedResponse create(UserPrincipal principal, CreateFeedRequest request) {
		if (request.getMedia() == null || request.getMedia().isEmpty()) {
			throw new BusinessException(ErrorCode.FEED_MEDIA_REQUIRED);
		}

		String visibility = normalizeVisibility(request.getVisibility());
		Feed feed = new Feed();
		feed.setUserNo(principal.getUserNo());
		feed.setContent(blankToNull(request.getContent()));
		feed.setVisibility(visibility);
		feedMapper.insertFeed(feed);

		insertMedia(feed.getFeedId(), principal.getUserNo(), request.getMedia());
		insertPets(feed.getFeedId(), principal.getUserNo(), request.getPets());
		insertTags(feed.getFeedId(), request.getTagIds());

		log.info("[create] feedId={} userNo={} media={}",
				feed.getFeedId(), principal.getUserNo(), request.getMedia().size());
		return toResponse(requireVisible(feed.getFeedId(), principal.getUserNo()), principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public FeedResponse getDetail(UserPrincipal principal, Long feedId) {
		Long viewerUserNo = principal == null ? null : principal.getUserNo();
		return toResponse(requireVisible(feedId, viewerUserNo), viewerUserNo);
	}

	@Transactional(readOnly = true)
	public List<FeedResponse> toResponses(List<Feed> feeds, Long viewerUserNo) {
		if (feeds == null || feeds.isEmpty()) {
			return List.of();
		}
		Set<Long> fileIds = new HashSet<>();
		Map<Long, List<FeedMedia>> mediaByFeed = new HashMap<>();
		Map<Long, User> authors = new HashMap<>();
		for (Feed feed : feeds) {
			authors.computeIfAbsent(feed.getUserNo(), userMapper::findActiveByUserNo);
			collectAuthorProfileId(fileIds, authors.get(feed.getUserNo()));
			List<FeedMedia> mediaList = feedMapper.findMediaByFeedId(feed.getFeedId());
			mediaByFeed.put(feed.getFeedId(), mediaList);
			for (FeedMedia media : mediaList) {
				if (media.getFileId() != null) {
					fileIds.add(media.getFileId());
				}
				if (media.getThumbnailFileId() != null) {
					fileIds.add(media.getThumbnailFileId());
				}
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		List<FeedResponse> result = new ArrayList<>();
		for (Feed feed : feeds) {
			result.add(toResponse(
					feed,
					viewerUserNo,
					authors.get(feed.getUserNo()),
					mediaByFeed.get(feed.getFeedId()),
					files));
		}
		return result;
	}

	@Transactional
	public FeedResponse update(UserPrincipal principal, Long feedId, UpdateFeedRequest request) {
		requireAuthor(feedId, principal.getUserNo());

		Feed patch = new Feed();
		patch.setFeedId(feedId);
		patch.setUserNo(principal.getUserNo());
		patch.setClearContent(Boolean.TRUE.equals(request.getClearContent()));
		if (!Boolean.TRUE.equals(request.getClearContent()) && request.getContent() != null) {
			patch.setContent(request.getContent().trim());
		}
		if (request.getVisibility() != null) {
			patch.setVisibility(normalizeVisibility(request.getVisibility()));
		}

		int updated = feedMapper.updateFeed(patch);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		log.info("[update] feedId={} userNo={}", feedId, principal.getUserNo());
		return toResponse(requireVisible(feedId, principal.getUserNo()), principal.getUserNo());
	}

	@Transactional
	public void delete(UserPrincipal principal, Long feedId) {
		Feed feed = requireActive(feedId);
		if (!canDelete(feed, principal.getUserNo())) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		int deleted = feedMapper.softDeleteFeed(feedId, principal.getUserNo());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		log.info("[delete] feedId={} byUserNo={}", feedId, principal.getUserNo());
	}

	@Transactional
	public FeedResponse like(UserPrincipal principal, Long feedId) {
		requireVisible(feedId, principal.getUserNo());
		FeedLike existing = feedMapper.findLike(feedId, principal.getUserNo());
		if (existing == null) {
			feedMapper.insertLike(feedId, principal.getUserNo());
			feedMapper.adjustLikeCount(feedId, 1);
		} else if (Boolean.TRUE.equals(existing.getIsDeleted())) {
			if (feedMapper.restoreLike(feedId, principal.getUserNo()) > 0) {
				feedMapper.adjustLikeCount(feedId, 1);
			}
		}
		log.info("[like] feedId={} userNo={}", feedId, principal.getUserNo());
		return toResponse(requireVisible(feedId, principal.getUserNo()), principal.getUserNo());
	}

	@Transactional
	public FeedResponse unlike(UserPrincipal principal, Long feedId) {
		requireVisible(feedId, principal.getUserNo());
		if (feedMapper.softDeleteLike(feedId, principal.getUserNo()) > 0) {
			feedMapper.adjustLikeCount(feedId, -1);
		}
		log.info("[unlike] feedId={} userNo={}", feedId, principal.getUserNo());
		return toResponse(requireVisible(feedId, principal.getUserNo()), principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public List<FeedLikerResponse> getLikers(UserPrincipal principal, Long feedId, Integer limit) {
		requireVisible(feedId, principal.getUserNo());
		return feedMapper.findActiveLikers(feedId, resolveLimit(limit)).stream()
				.map(like -> FeedLikerResponse.builder()
						.userNo(like.getUserNo())
						.nickname(like.getNickname())
						.likedAt(like.getCreatedAt())
						.build())
				.toList();
	}

	@Transactional
	public FeedResponse store(UserPrincipal principal, Long feedId) {
		requireVisible(feedId, principal.getUserNo());
		FeedStore existing = feedMapper.findStore(feedId, principal.getUserNo());
		if (existing == null) {
			feedMapper.insertStore(feedId, principal.getUserNo());
			feedMapper.adjustStoreCount(feedId, 1);
		} else if (Boolean.TRUE.equals(existing.getIsDeleted())) {
			if (feedMapper.restoreStore(feedId, principal.getUserNo()) > 0) {
				feedMapper.adjustStoreCount(feedId, 1);
			}
		}
		log.info("[store] feedId={} userNo={}", feedId, principal.getUserNo());
		return toResponse(requireVisible(feedId, principal.getUserNo()), principal.getUserNo());
	}

	@Transactional
	public FeedResponse unstore(UserPrincipal principal, Long feedId) {
		requireVisible(feedId, principal.getUserNo());
		if (feedMapper.softDeleteStore(feedId, principal.getUserNo()) > 0) {
			feedMapper.adjustStoreCount(feedId, -1);
		}
		log.info("[unstore] feedId={} userNo={}", feedId, principal.getUserNo());
		return toResponse(requireVisible(feedId, principal.getUserNo()), principal.getUserNo());
	}

	@Transactional(readOnly = true)
	public FeedShareResponse share(UserPrincipal principal, Long feedId) {
		requireVisible(feedId, principal.getUserNo());
		return FeedShareResponse.builder()
				.feedId(feedId)
				.path("/feeds/" + feedId)
				.build();
	}

	@Transactional(readOnly = true)
	public List<FeedCommentResponse> getComments(UserPrincipal principal, Long feedId, Integer limit) {
		requireVisible(feedId, principal.getUserNo());
		List<FeedComment> comments = feedMapper.findActiveComments(feedId, resolveLimit(limit));
		int totalThreadCount = feedMapper.countActiveCommentsAndReplies(feedId);
		boolean embedReplies = totalThreadCount <= INLINE_REPLY_THRESHOLD;
		Long viewerUserNo = principal.getUserNo();
		Set<Long> likedCommentIds = activeLikedCommentIds(
				comments.stream().map(FeedComment::getFeedCommentId).toList(),
				viewerUserNo);
		Map<Long, List<FeedReply>> repliesByComment = new HashMap<>();
		Set<Long> profileIds = new HashSet<>();
		for (FeedComment comment : comments) {
			collectProfileId(profileIds, comment.getAuthorProfileFileId());
			if (embedReplies) {
				List<FeedReply> replyRows = feedMapper.findActiveReplies(comment.getFeedCommentId(), MAX_LIST_LIMIT);
				repliesByComment.put(comment.getFeedCommentId(), replyRows);
				for (FeedReply reply : replyRows) {
					collectProfileId(profileIds, reply.getAuthorProfileFileId());
				}
			}
		}
		Map<Long, FileSummaryResponse> profiles = fileService.findSummaries(profileIds);
		return comments.stream()
				.map(comment -> toCommentResponse(
						comment,
						embedReplies,
						viewerUserNo,
						likedCommentIds,
						profiles,
						embedReplies
								? repliesByComment.getOrDefault(comment.getFeedCommentId(), List.of())
								: null))
				.toList();
	}

	@Transactional
	public FeedCommentResponse createComment(
			UserPrincipal principal, Long feedId, CreateCommentRequest request) {
		requireVisible(feedId, principal.getUserNo());
		FeedComment comment = new FeedComment();
		comment.setFeedId(feedId);
		comment.setUserNo(principal.getUserNo());
		comment.setContent(request.getContent().trim());
		feedMapper.insertComment(comment);
		insertCommentMentions(comment.getFeedCommentId(), null, request.getMentions());
		feedMapper.adjustCommentCount(feedId, 1);
		log.info("[createComment] feedId={} commentId={}", feedId, comment.getFeedCommentId());
		return toCommentResponse(requireComment(feedId, comment.getFeedCommentId()), false, principal.getUserNo(), null);
	}

	@Transactional
	public FeedCommentResponse updateComment(
			UserPrincipal principal, Long feedId, Long commentId, UpdateCommentRequest request) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		int updated = feedMapper.updateCommentContent(commentId, principal.getUserNo(), request.getContent().trim());
		if (updated == 0) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return toCommentResponse(requireComment(feedId, commentId), false, principal.getUserNo(), null);
	}

	@Transactional
	public void deleteComment(UserPrincipal principal, Long feedId, Long commentId) {
		Feed feed = requireVisible(feedId, principal.getUserNo());
		FeedComment comment = requireComment(feedId, commentId);
		int deleted;
		if (comment.getUserNo().equals(principal.getUserNo())) {
			deleted = feedMapper.softDeleteComment(commentId, principal.getUserNo());
		} else if (feed.getUserNo().equals(principal.getUserNo())) {
			deleted = feedMapper.softDeleteCommentByFeedAuthor(commentId, feedId, principal.getUserNo());
		} else {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.FEED_COMMENT_NOT_FOUND);
		}
		feedMapper.adjustCommentCount(feedId, -1);
		log.info("[deleteComment] feedId={} commentId={} by={}", feedId, commentId, principal.getUserNo());
	}

	@Transactional
	public FeedCommentResponse likeComment(UserPrincipal principal, Long feedId, Long commentId) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		FeedCommentLike existing = feedMapper.findCommentLike(commentId, principal.getUserNo());
		if (existing == null) {
			feedMapper.insertCommentLike(commentId, principal.getUserNo());
			feedMapper.adjustCommentLikeCount(commentId, 1);
		} else if (Boolean.TRUE.equals(existing.getIsDeleted())) {
			if (feedMapper.restoreCommentLike(commentId, principal.getUserNo()) > 0) {
				feedMapper.adjustCommentLikeCount(commentId, 1);
			}
		}
		log.info("[likeComment] feedId={} commentId={} userNo={}", feedId, commentId, principal.getUserNo());
		return toCommentResponse(requireComment(feedId, commentId), false, principal.getUserNo(), null);
	}

	@Transactional
	public FeedCommentResponse unlikeComment(UserPrincipal principal, Long feedId, Long commentId) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		if (feedMapper.softDeleteCommentLike(commentId, principal.getUserNo()) > 0) {
			feedMapper.adjustCommentLikeCount(commentId, -1);
		}
		log.info("[unlikeComment] feedId={} commentId={} userNo={}", feedId, commentId, principal.getUserNo());
		return toCommentResponse(requireComment(feedId, commentId), false, principal.getUserNo(), null);
	}

	@Transactional(readOnly = true)
	public List<FeedReplyResponse> getReplies(
			UserPrincipal principal, Long feedId, Long commentId, Integer limit) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		List<FeedReply> replies = feedMapper.findActiveReplies(commentId, resolveLimit(limit));
		Set<Long> likedReplyIds = activeLikedReplyIds(
				replies.stream().map(FeedReply::getFeedReplyId).toList(),
				principal.getUserNo());
		Set<Long> profileIds = new HashSet<>();
		for (FeedReply reply : replies) {
			collectProfileId(profileIds, reply.getAuthorProfileFileId());
		}
		Map<Long, FileSummaryResponse> profiles = fileService.findSummaries(profileIds);
		return replies.stream()
				.map(reply -> toReplyResponse(reply, principal.getUserNo(), likedReplyIds, profiles))
				.toList();
	}

	@Transactional
	public FeedReplyResponse createReply(
			UserPrincipal principal, Long feedId, Long commentId, CreateReplyRequest request) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		FeedReply reply = new FeedReply();
		reply.setFeedCommentId(commentId);
		reply.setUserNo(principal.getUserNo());
		reply.setContent(request.getContent().trim());
		feedMapper.insertReply(reply);
		insertCommentMentions(null, reply.getFeedReplyId(), request.getMentions());
		log.info("[createReply] commentId={} replyId={}", commentId, reply.getFeedReplyId());
		return toReplyResponse(requireReply(commentId, reply.getFeedReplyId()), principal.getUserNo(), null);
	}

	@Transactional
	public FeedReplyResponse updateReply(
			UserPrincipal principal,
			Long feedId,
			Long commentId,
			Long replyId,
			UpdateReplyRequest request) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		requireReply(commentId, replyId);
		int updated = feedMapper.updateReplyContent(replyId, principal.getUserNo(), request.getContent().trim());
		if (updated == 0) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return toReplyResponse(requireReply(commentId, replyId), principal.getUserNo(), null);
	}

	@Transactional
	public FeedReplyResponse likeReply(
			UserPrincipal principal, Long feedId, Long commentId, Long replyId) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		requireReply(commentId, replyId);
		FeedReplyLike existing = feedMapper.findReplyLike(replyId, principal.getUserNo());
		if (existing == null) {
			feedMapper.insertReplyLike(replyId, principal.getUserNo());
			feedMapper.adjustReplyLikeCount(replyId, 1);
		} else if (Boolean.TRUE.equals(existing.getIsDeleted())) {
			if (feedMapper.restoreReplyLike(replyId, principal.getUserNo()) > 0) {
				feedMapper.adjustReplyLikeCount(replyId, 1);
			}
		}
		log.info("[likeReply] replyId={} userNo={}", replyId, principal.getUserNo());
		return toReplyResponse(requireReply(commentId, replyId), principal.getUserNo(), null);
	}

	@Transactional
	public FeedReplyResponse unlikeReply(
			UserPrincipal principal, Long feedId, Long commentId, Long replyId) {
		requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		requireReply(commentId, replyId);
		if (feedMapper.softDeleteReplyLike(replyId, principal.getUserNo()) > 0) {
			feedMapper.adjustReplyLikeCount(replyId, -1);
		}
		log.info("[unlikeReply] replyId={} userNo={}", replyId, principal.getUserNo());
		return toReplyResponse(requireReply(commentId, replyId), principal.getUserNo(), null);
	}

	@Transactional
	public void deleteReply(UserPrincipal principal, Long feedId, Long commentId, Long replyId) {
		Feed feed = requireVisible(feedId, principal.getUserNo());
		requireComment(feedId, commentId);
		FeedReply reply = requireReply(commentId, replyId);
		int deleted;
		if (reply.getUserNo().equals(principal.getUserNo())) {
			deleted = feedMapper.softDeleteReply(replyId, principal.getUserNo());
		} else if (feed.getUserNo().equals(principal.getUserNo())) {
			deleted = feedMapper.softDeleteReplyByFeedAuthor(replyId, commentId, principal.getUserNo());
		} else {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.FEED_REPLY_NOT_FOUND);
		}
		log.info("[deleteReply] replyId={} by={}", replyId, principal.getUserNo());
	}

	private void insertMedia(Long feedId, Long userNo, List<CreateFeedRequest.MediaItem> mediaItems) {
		int order = 0;
		for (CreateFeedRequest.MediaItem item : mediaItems) {
			FileMeta file = fileService.requireActive(item.getFileId());
			String mediaType = resolveMediaType(item.getMediaType(), file);
			if (item.getThumbnailFileId() != null) {
				fileService.requireActive(item.getThumbnailFileId());
			}
			if ("IMAGE".equals(mediaType)) {
				item.setThumbnailFileId(null);
				item.setDurationSec(null);
			}

			FeedMedia media = new FeedMedia();
			media.setFeedId(feedId);
			media.setFileId(item.getFileId());
			media.setMediaType(mediaType);
			media.setThumbnailFileId(item.getThumbnailFileId());
			media.setDurationSec(item.getDurationSec());
			media.setSortOrder(order++);
			feedMapper.insertMedia(media);

			if (item.getMentions() != null) {
				Set<Long> seenPets = new HashSet<>();
				for (CreateFeedRequest.MentionItem mentionItem : item.getMentions()) {
					if (mentionItem.getPetId() == null || !seenPets.add(mentionItem.getPetId())) {
						continue;
					}
					requirePet(mentionItem.getPetId());
					FeedMediaMention mention = new FeedMediaMention();
					mention.setFeedMediaId(media.getFeedMediaId());
					mention.setPetId(mentionItem.getPetId());
					mention.setPosX(mentionItem.getPosX());
					mention.setPosY(mentionItem.getPosY());
					mention.setAddedByUserNo(userNo);
					feedMapper.insertMediaMention(mention);
				}
			}
		}
	}

	private void insertPets(Long feedId, Long userNo, List<CreateFeedRequest.PetItem> pets) {
		if (pets == null || pets.isEmpty()) {
			return;
		}
		Set<Long> seen = new HashSet<>();
		boolean mainAssigned = false;
		for (CreateFeedRequest.PetItem item : pets) {
			if (item.getPetId() == null || !seen.add(item.getPetId())) {
				continue;
			}
			requirePet(item.getPetId());
			String role = StringUtils.hasText(item.getRole()) ? item.getRole() : "TAGGED";
			boolean isMain = Boolean.TRUE.equals(item.getIsMain()) && !mainAssigned;
			if (isMain) {
				mainAssigned = true;
			}
			FeedPet feedPet = new FeedPet();
			feedPet.setFeedId(feedId);
			feedPet.setPetId(item.getPetId());
			feedPet.setAddedByUserNo(userNo);
			feedPet.setRole(role);
			feedPet.setIsMain(isMain);
			feedMapper.insertFeedPet(feedPet);
		}
	}

	private void insertTags(Long feedId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return;
		}
		Set<Long> seen = new HashSet<>();
		for (Long tagId : tagIds) {
			if (tagId == null || !seen.add(tagId)) {
				continue;
			}
			Tag tag = tagMapper.findActiveByTagId(tagId);
			if (tag == null) {
				throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
			}
			feedMapper.insertFeedTag(feedId, tagId);
			tagMapper.incrementUseCount(tagId);
		}
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

	private String normalizeVisibility(String visibility) {
		if (!StringUtils.hasText(visibility)) {
			return "PUBLIC";
		}
		String value = visibility.trim().toUpperCase(Locale.ROOT);
		if (!Set.of("PUBLIC", "NEIGHBORS", "PRIVATE").contains(value)) {
			throw new BusinessException(ErrorCode.FEED_INVALID_VISIBILITY);
		}
		return value;
	}

	private boolean canDelete(Feed feed, Long userNo) {
		if (feed.getUserNo().equals(userNo)) {
			return true;
		}
		return feedMapper.countCollaboratorOwnedByUser(feed.getFeedId(), userNo) > 0;
	}

	private Feed requireActive(Long feedId) {
		Feed feed = feedMapper.findActiveByFeedId(feedId);
		if (feed == null) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		return feed;
	}

	private Feed requireVisible(Long feedId, Long viewerUserNo) {
		Feed feed = requireActive(feedId);
		if (viewerUserNo == null) {
			if (!"PUBLIC".equals(feed.getVisibility())) {
				throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
			}
			return feed;
		}
		if ("PRIVATE".equals(feed.getVisibility()) && !feed.getUserNo().equals(viewerUserNo)) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		if ("NEIGHBORS".equals(feed.getVisibility())
				&& !feed.getUserNo().equals(viewerUserNo)
				&& !neighborService.isAcceptedPair(viewerUserNo, feed.getUserNo())) {
			throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		}
		return feed;
	}

	private Feed requireAuthor(Long feedId, Long userNo) {
		Feed feed = requireActive(feedId);
		if (!feed.getUserNo().equals(userNo)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return feed;
	}

	private Pet requirePet(Long petId) {
		Pet pet = petMapper.findActiveByPetId(petId);
		if (pet == null) {
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		}
		return pet;
	}

	private FeedComment requireComment(Long feedId, Long commentId) {
		FeedComment comment = feedMapper.findActiveComment(feedId, commentId);
		if (comment == null) {
			throw new BusinessException(ErrorCode.FEED_COMMENT_NOT_FOUND);
		}
		return comment;
	}

	private FeedReply requireReply(Long commentId, Long replyId) {
		FeedReply reply = feedMapper.findActiveReply(commentId, replyId);
		if (reply == null) {
			throw new BusinessException(ErrorCode.FEED_REPLY_NOT_FOUND);
		}
		return reply;
	}

	private FeedResponse toResponse(Feed feed, Long viewerUserNo) {
		User author = userMapper.findActiveByUserNo(feed.getUserNo());
		List<FeedMedia> mediaList = feedMapper.findMediaByFeedId(feed.getFeedId());
		Set<Long> fileIds = new HashSet<>();
		collectAuthorProfileId(fileIds, author);
		for (FeedMedia media : mediaList) {
			if (media.getFileId() != null) {
				fileIds.add(media.getFileId());
			}
			if (media.getThumbnailFileId() != null) {
				fileIds.add(media.getThumbnailFileId());
			}
		}
		return toResponse(feed, viewerUserNo, author, mediaList, fileService.findSummaries(fileIds));
	}

	private static void collectAuthorProfileId(Set<Long> fileIds, User author) {
		if (author != null && author.getProfileFileId() != null) {
			fileIds.add(author.getProfileFileId().longValue());
		}
	}

	private FeedResponse toResponse(
			Feed feed,
			Long viewerUserNo,
			User author,
			List<FeedMedia> mediaList,
			Map<Long, FileSummaryResponse> files) {
		List<FeedResponse.MediaItem> mediaItems = new ArrayList<>();
		List<FeedMedia> resolvedMedia = mediaList != null ? mediaList : List.of();
		for (FeedMedia media : resolvedMedia) {
			List<FeedResponse.MentionItem> mentions = feedMapper.findMentionsByFeedMediaId(media.getFeedMediaId())
					.stream()
					.map(m -> FeedResponse.MentionItem.builder()
							.petId(m.getPetId())
							.posX(m.getPosX())
							.posY(m.getPosY())
							.build())
					.toList();
			mediaItems.add(FeedResponse.MediaItem.builder()
					.feedMediaId(media.getFeedMediaId())
					.fileId(media.getFileId())
					.file(files.get(media.getFileId()))
					.mediaType(media.getMediaType())
					.thumbnailFileId(media.getThumbnailFileId())
					.thumbnailFile(media.getThumbnailFileId() == null
							? null
							: files.get(media.getThumbnailFileId()))
					.durationSec(media.getDurationSec())
					.sortOrder(media.getSortOrder())
					.mentions(mentions)
					.build());
		}

		List<FeedResponse.PetItem> pets = feedMapper.findPetsByFeedId(feed.getFeedId()).stream()
				.map(p -> FeedResponse.PetItem.builder()
						.petId(p.getPetId())
						.role(p.getRole())
						.isMain(p.getIsMain())
						.addedByUserNo(p.getAddedByUserNo())
						.build())
				.toList();

		boolean likedByMe = false;
		boolean storedByMe = false;
		if (viewerUserNo != null) {
			FeedLike like = feedMapper.findLike(feed.getFeedId(), viewerUserNo);
			likedByMe = like != null && !Boolean.TRUE.equals(like.getIsDeleted());
			FeedStore store = feedMapper.findStore(feed.getFeedId(), viewerUserNo);
			storedByMe = store != null && !Boolean.TRUE.equals(store.getIsDeleted());
		}

		Integer authorProfileFileId = author != null ? author.getProfileFileId() : null;
		return FeedResponse.builder()
				.feedId(feed.getFeedId())
				.userNo(feed.getUserNo())
				.authorNickname(author != null ? author.getNickname() : null)
				.authorProfileFile(authorProfileFileId == null
						? null
						: files.get(authorProfileFileId.longValue()))
				.content(feed.getContent())
				.visibility(feed.getVisibility())
				.likeCount(feed.getLikeCount())
				.commentCount(feed.getCommentCount())
				.storeCount(feed.getStoreCount())
				.likedByMe(likedByMe)
				.storedByMe(storedByMe)
				.media(mediaItems)
				.pets(pets)
				.tagIds(feedMapper.findTagIdsByFeedId(feed.getFeedId()))
				.createdAt(feed.getCreatedAt())
				.updatedAt(feed.getUpdatedAt())
				.build();
	}

	private void insertCommentMentions(
			Long commentId,
			Long replyId,
			List<CommentMentionRequest> mentions) {
		if (mentions == null || mentions.isEmpty()) {
			return;
		}
		Set<String> seen = new HashSet<>();
		for (CommentMentionRequest item : mentions) {
			if (item == null) {
				continue;
			}
			FeedCommentMention mention = new FeedCommentMention();
			mention.setFeedCommentId(commentId);
			mention.setFeedReplyId(replyId);
			if (item.getUserNo() != null) {
				String key = "U:" + item.getUserNo();
				if (!seen.add(key)) {
					continue;
				}
				User user = userMapper.findActiveByUserNo(item.getUserNo());
				if (user == null) {
					throw new BusinessException(ErrorCode.USER_NOT_FOUND);
				}
				mention.setMentionedUserNo(item.getUserNo());
			} else if (item.getPetId() != null) {
				String key = "P:" + item.getPetId();
				if (!seen.add(key)) {
					continue;
				}
				requirePet(item.getPetId());
				mention.setMentionedPetId(item.getPetId());
			} else {
				throw new BusinessException(ErrorCode.INVALID_INPUT, "멘션은 userNo 또는 petId가 필요합니다.");
			}
			feedMapper.insertCommentMention(mention);
		}
	}

	private List<CommentMentionResponse> toMentionResponses(List<FeedCommentMention> mentions) {
		if (mentions == null || mentions.isEmpty()) {
			return List.of();
		}
		List<CommentMentionResponse> result = new ArrayList<>();
		for (FeedCommentMention mention : mentions) {
			if (mention.getMentionedUserNo() != null) {
				User user = userMapper.findActiveByUserNo(mention.getMentionedUserNo());
				result.add(CommentMentionResponse.builder()
						.type("USER")
						.userNo(mention.getMentionedUserNo())
						.nickname(user != null ? user.getNickname() : null)
						.build());
			} else if (mention.getMentionedPetId() != null) {
				Pet pet = petMapper.findActiveByPetId(mention.getMentionedPetId());
				result.add(CommentMentionResponse.builder()
						.type("PET")
						.petId(mention.getMentionedPetId())
						.mentionId(pet != null ? pet.getMentionId() : null)
						.petName(pet != null ? pet.getName() : null)
						.build());
			}
		}
		return result;
	}

	private FeedCommentResponse toCommentResponse(
			FeedComment comment,
			boolean embedReplies,
			Long viewerUserNo,
			Set<Long> likedCommentIds) {
		return toCommentResponse(comment, embedReplies, viewerUserNo, likedCommentIds, null, null);
	}

	private FeedCommentResponse toCommentResponse(
			FeedComment comment,
			boolean embedReplies,
			Long viewerUserNo,
			Set<Long> likedCommentIds,
			Map<Long, FileSummaryResponse> profiles,
			List<FeedReply> preloadedReplies) {
		AuthorProfile author = resolveAuthor(
				comment.getUserNo(),
				comment.getAuthorNickname(),
				comment.getAuthorProfileFileId(),
				profiles);
		int replyCount = feedMapper.countActiveRepliesByCommentId(comment.getFeedCommentId());
		List<FeedReplyResponse> replies = null;
		if (embedReplies) {
			List<FeedReply> replyRows = preloadedReplies != null
					? preloadedReplies
					: feedMapper.findActiveReplies(comment.getFeedCommentId(), MAX_LIST_LIMIT);
			Set<Long> likedReplyIds = activeLikedReplyIds(
					replyRows.stream().map(FeedReply::getFeedReplyId).toList(),
					viewerUserNo);
			replies = replyRows.stream()
					.map(reply -> toReplyResponse(reply, viewerUserNo, likedReplyIds, profiles))
					.toList();
		}
		boolean likedByMe = likedCommentIds != null
				? likedCommentIds.contains(comment.getFeedCommentId())
				: isCommentLikedBy(comment.getFeedCommentId(), viewerUserNo);
		return FeedCommentResponse.builder()
				.feedCommentId(comment.getFeedCommentId())
				.feedId(comment.getFeedId())
				.userNo(comment.getUserNo())
				.authorNickname(author.nickname())
				.authorProfileFile(author.profileFile())
				.content(comment.getContent())
				.likeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount())
				.likedByMe(likedByMe)
				.mentions(toMentionResponses(feedMapper.findMentionsByCommentId(comment.getFeedCommentId())))
				.replyCount(replyCount)
				.replies(replies)
				.createdAt(comment.getCreatedAt())
				.updatedAt(comment.getUpdatedAt())
				.build();
	}

	private FeedReplyResponse toReplyResponse(FeedReply reply, Long viewerUserNo, Set<Long> likedReplyIds) {
		return toReplyResponse(reply, viewerUserNo, likedReplyIds, null);
	}

	private FeedReplyResponse toReplyResponse(
			FeedReply reply,
			Long viewerUserNo,
			Set<Long> likedReplyIds,
			Map<Long, FileSummaryResponse> profiles) {
		AuthorProfile author = resolveAuthor(
				reply.getUserNo(),
				reply.getAuthorNickname(),
				reply.getAuthorProfileFileId(),
				profiles);
		boolean likedByMe = likedReplyIds != null
				? likedReplyIds.contains(reply.getFeedReplyId())
				: isReplyLikedBy(reply.getFeedReplyId(), viewerUserNo);
		return FeedReplyResponse.builder()
				.feedReplyId(reply.getFeedReplyId())
				.feedCommentId(reply.getFeedCommentId())
				.userNo(reply.getUserNo())
				.authorNickname(author.nickname())
				.authorProfileFile(author.profileFile())
				.content(reply.getContent())
				.likeCount(reply.getLikeCount() == null ? 0 : reply.getLikeCount())
				.likedByMe(likedByMe)
				.mentions(toMentionResponses(feedMapper.findMentionsByReplyId(reply.getFeedReplyId())))
				.createdAt(reply.getCreatedAt())
				.updatedAt(reply.getUpdatedAt())
				.build();
	}

	private static void collectProfileId(Set<Long> profileIds, Integer profileFileId) {
		if (profileFileId != null) {
			profileIds.add(profileFileId.longValue());
		}
	}

	private AuthorProfile resolveAuthor(
			Long userNo,
			String nickname,
			Integer profileFileId,
			Map<Long, FileSummaryResponse> profiles) {
		if (nickname == null) {
			User user = userMapper.findActiveByUserNo(userNo);
			nickname = user != null ? user.getNickname() : null;
			profileFileId = user != null ? user.getProfileFileId() : null;
		}
		if (profileFileId == null) {
			return new AuthorProfile(nickname, null);
		}
		Long fileId = profileFileId.longValue();
		FileSummaryResponse profileFile = profiles != null
				? profiles.get(fileId)
				: fileService.findSummary(profileFileId);
		return new AuthorProfile(nickname, profileFile);
	}

	private record AuthorProfile(String nickname, FileSummaryResponse profileFile) {
	}

	private boolean isCommentLikedBy(Long commentId, Long viewerUserNo) {
		if (viewerUserNo == null) {
			return false;
		}
		FeedCommentLike like = feedMapper.findCommentLike(commentId, viewerUserNo);
		return like != null && !Boolean.TRUE.equals(like.getIsDeleted());
	}

	private boolean isReplyLikedBy(Long replyId, Long viewerUserNo) {
		if (viewerUserNo == null) {
			return false;
		}
		FeedReplyLike like = feedMapper.findReplyLike(replyId, viewerUserNo);
		return like != null && !Boolean.TRUE.equals(like.getIsDeleted());
	}

	private Set<Long> activeLikedCommentIds(List<Long> commentIds, Long viewerUserNo) {
		if (viewerUserNo == null || commentIds == null || commentIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(feedMapper.findActiveLikedCommentIds(commentIds, viewerUserNo));
	}

	private Set<Long> activeLikedReplyIds(List<Long> replyIds, Long viewerUserNo) {
		if (viewerUserNo == null || replyIds == null || replyIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(feedMapper.findActiveLikedReplyIds(replyIds, viewerUserNo));
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIST_LIMIT;
		}
		return Math.min(limit, MAX_LIST_LIMIT);
	}

	private static String blankToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
