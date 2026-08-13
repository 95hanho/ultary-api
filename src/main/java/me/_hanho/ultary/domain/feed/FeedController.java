package me._hanho.ultary.domain.feed;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.feed.dto.request.CreateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.CreateReplyRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateCommentRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateReplyRequest;
import me._hanho.ultary.domain.feed.dto.response.FeedCommentResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedLikerResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedReplyResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.dto.response.FeedShareResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** 경로 원본: springEndpoints.feeds / api-memo.md §5 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

	private final FeedService feedService;

	@PostMapping
	public ApiResponse<FeedResponse> create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody CreateFeedRequest request) {
		log.info("[create] mediaCount={} visibility={}",
				request.getMedia() != null ? request.getMedia().size() : 0,
				request.getVisibility());
		return ApiResponse.ok(feedService.create(principal, request), "게시글 등록 성공");
	}

	@GetMapping("/{feedId}")
	public ApiResponse<FeedResponse> detail(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[detail] feedId={}", feedId);
		return ApiResponse.ok(feedService.getDetail(principal, feedId), "게시글 조회 성공");
	}

	@PatchMapping("/{feedId}")
	public ApiResponse<FeedResponse> update(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@Valid @RequestBody UpdateFeedRequest request) {
		log.info("[update] feedId={}", feedId);
		return ApiResponse.ok(feedService.update(principal, feedId, request), "게시글 수정 성공");
	}

	@DeleteMapping("/{feedId}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[delete] feedId={}", feedId);
		feedService.delete(principal, feedId);
		return ApiResponse.okEmpty("게시글 삭제 성공");
	}

	@PostMapping("/{feedId}/like")
	public ApiResponse<FeedResponse> like(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[like] feedId={}", feedId);
		return ApiResponse.ok(feedService.like(principal, feedId), "좋아요 성공");
	}

	@DeleteMapping("/{feedId}/like")
	public ApiResponse<FeedResponse> unlike(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[unlike] feedId={}", feedId);
		return ApiResponse.ok(feedService.unlike(principal, feedId), "좋아요 취소 성공");
	}

	@GetMapping("/{feedId}/likers")
	public ApiResponse<List<FeedLikerResponse>> likers(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@RequestParam(required = false) Integer limit) {
		log.info("[likers] feedId={} limit={}", feedId, limit);
		return ApiResponse.ok(feedService.getLikers(principal, feedId, limit), "좋아요 목록 조회 성공");
	}

	@PostMapping("/{feedId}/store")
	public ApiResponse<FeedResponse> store(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[store] feedId={}", feedId);
		return ApiResponse.ok(feedService.store(principal, feedId), "게시글 저장 성공");
	}

	@DeleteMapping("/{feedId}/store")
	public ApiResponse<FeedResponse> unstore(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[unstore] feedId={}", feedId);
		return ApiResponse.ok(feedService.unstore(principal, feedId), "게시글 저장 해제 성공");
	}

	@PostMapping("/{feedId}/share")
	public ApiResponse<FeedShareResponse> share(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId) {
		log.info("[share] feedId={}", feedId);
		return ApiResponse.ok(feedService.share(principal, feedId), "공유 경로 조회 성공");
	}

	/** 댓글 목록. 댓글+답글 합 ≤10이면 replies 포함, 초과면 replies=null(펼치기 API 사용) */
	@GetMapping("/{feedId}/comments")
	public ApiResponse<List<FeedCommentResponse>> comments(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@RequestParam(required = false) Integer limit) {
		log.info("[comments] feedId={} limit={}", feedId, limit);
		return ApiResponse.ok(feedService.getComments(principal, feedId, limit), "댓글 목록 조회 성공");
	}

	@PostMapping("/{feedId}/comments")
	public ApiResponse<FeedCommentResponse> createComment(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@Valid @RequestBody CreateCommentRequest request) {
		log.info("[createComment] feedId={}", feedId);
		return ApiResponse.ok(feedService.createComment(principal, feedId, request), "댓글 작성 성공");
	}

	@PatchMapping("/{feedId}/comments/{commentId}")
	public ApiResponse<FeedCommentResponse> updateComment(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@Valid @RequestBody UpdateCommentRequest request) {
		log.info("[updateComment] feedId={} commentId={}", feedId, commentId);
		return ApiResponse.ok(feedService.updateComment(principal, feedId, commentId, request), "댓글 수정 성공");
	}

	@DeleteMapping("/{feedId}/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId) {
		log.info("[deleteComment] feedId={} commentId={}", feedId, commentId);
		feedService.deleteComment(principal, feedId, commentId);
		return ApiResponse.okEmpty("댓글 삭제 성공");
	}

	@GetMapping("/{feedId}/comments/{commentId}/replies")
	public ApiResponse<List<FeedReplyResponse>> replies(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@RequestParam(required = false) Integer limit) {
		log.info("[replies] feedId={} commentId={} limit={}", feedId, commentId, limit);
		return ApiResponse.ok(feedService.getReplies(principal, feedId, commentId, limit), "답글 목록 조회 성공");
	}

	@PostMapping("/{feedId}/comments/{commentId}/replies")
	public ApiResponse<FeedReplyResponse> createReply(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@Valid @RequestBody CreateReplyRequest request) {
		log.info("[createReply] feedId={} commentId={}", feedId, commentId);
		return ApiResponse.ok(feedService.createReply(principal, feedId, commentId, request), "답글 작성 성공");
	}

	@PatchMapping("/{feedId}/comments/{commentId}/replies/{replyId}")
	public ApiResponse<FeedReplyResponse> updateReply(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@PathVariable Long replyId,
			@Valid @RequestBody UpdateReplyRequest request) {
		log.info("[updateReply] feedId={} commentId={} replyId={}", feedId, commentId, replyId);
		return ApiResponse.ok(
				feedService.updateReply(principal, feedId, commentId, replyId, request),
				"답글 수정 성공");
	}

	@DeleteMapping("/{feedId}/comments/{commentId}/replies/{replyId}")
	public ApiResponse<Void> deleteReply(
			@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@PathVariable Long replyId) {
		log.info("[deleteReply] feedId={} commentId={} replyId={}", feedId, commentId, replyId);
		feedService.deleteReply(principal, feedId, commentId, replyId);
		return ApiResponse.okEmpty("답글 삭제 성공");
	}
}
