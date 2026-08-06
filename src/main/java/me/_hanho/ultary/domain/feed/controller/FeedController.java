package me._hanho.ultary.domain.feed.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.feed.service.FeedService;

/** 경로 원본: springEndpoints.feeds / api-memo.md §5 */
@Slf4j
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

	private final FeedService feedService;

	// 게시글 등록
	@PostMapping
	public ApiResponse<Void> create() {
		log.info("[create]");
		feedService.create();
		return ApiResponse.ok();
	}

	// 게시글 상세
	@GetMapping("/{feedId}")
	public ApiResponse<Void> detail(@PathVariable Long feedId) {
		log.info("[detail] feedId={}", feedId);
		feedService.getDetail(feedId);
		return ApiResponse.ok();
	}

	// 게시글 수정
	@PatchMapping("/{feedId}")
	public ApiResponse<Void> update(@PathVariable Long feedId) {
		log.info("[update] feedId={}", feedId);
		feedService.update(feedId);
		return ApiResponse.ok();
	}

	// 게시글 삭제
	@DeleteMapping("/{feedId}")
	public ApiResponse<Void> delete(@PathVariable Long feedId) {
		log.info("[delete] feedId={}", feedId);
		feedService.delete(feedId);
		return ApiResponse.ok();
	}

	// 좋아요
	@PostMapping("/{feedId}/like")
	public ApiResponse<Void> like(@PathVariable Long feedId) {
		log.info("[like] feedId={}", feedId);
		feedService.like(feedId);
		return ApiResponse.ok();
	}

	// 좋아요 취소
	@DeleteMapping("/{feedId}/like")
	public ApiResponse<Void> unlike(@PathVariable Long feedId) {
		log.info("[unlike] feedId={}", feedId);
		feedService.unlike(feedId);
		return ApiResponse.ok();
	}

	// 좋아요한 사람 목록
	@GetMapping("/{feedId}/likers")
	public ApiResponse<Void> likers(@PathVariable Long feedId) {
		log.info("[likers] feedId={}", feedId);
		feedService.getLikers(feedId);
		return ApiResponse.ok();
	}

	// 게시글 저장
	@PostMapping("/{feedId}/store")
	public ApiResponse<Void> store(@PathVariable Long feedId) {
		log.info("[store] feedId={}", feedId);
		feedService.store(feedId);
		return ApiResponse.ok();
	}

	// 게시글 저장 해제
	@DeleteMapping("/{feedId}/store")
	public ApiResponse<Void> unstore(@PathVariable Long feedId) {
		log.info("[unstore] feedId={}", feedId);
		feedService.unstore(feedId);
		return ApiResponse.ok();
	}

	// 게시글 공유 (URL 복사·DM 전송)
	@PostMapping("/{feedId}/share")
	public ApiResponse<Void> share(@PathVariable Long feedId) {
		log.info("[share] feedId={}", feedId);
		feedService.share(feedId);
		return ApiResponse.ok();
	}

	// 댓글 목록
	@GetMapping("/{feedId}/comments")
	public ApiResponse<Void> comments(@PathVariable Long feedId) {
		log.info("[comments] feedId={}", feedId);
		feedService.getComments(feedId);
		return ApiResponse.ok();
	}

	// 댓글 작성
	@PostMapping("/{feedId}/comments")
	public ApiResponse<Void> createComment(@PathVariable Long feedId) {
		log.info("[createComment] feedId={}", feedId);
		feedService.createComment(feedId);
		return ApiResponse.ok();
	}

	// 댓글 수정
	@PatchMapping("/{feedId}/comments/{commentId}")
	public ApiResponse<Void> updateComment(
			@PathVariable Long feedId,
			@PathVariable Long commentId) {
		log.info("[updateComment] feedId={}, commentId={}", feedId, commentId);
		feedService.updateComment(feedId, commentId);
		return ApiResponse.ok();
	}

	// 댓글 삭제
	@DeleteMapping("/{feedId}/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
			@PathVariable Long feedId,
			@PathVariable Long commentId) {
		log.info("[deleteComment] feedId={}, commentId={}", feedId, commentId);
		feedService.deleteComment(feedId, commentId);
		return ApiResponse.ok();
	}

	// 답글 목록
	@GetMapping("/{feedId}/comments/{commentId}/replies")
	public ApiResponse<Void> replies(
			@PathVariable Long feedId,
			@PathVariable Long commentId) {
		log.info("[replies] feedId={}, commentId={}", feedId, commentId);
		feedService.getReplies(feedId, commentId);
		return ApiResponse.ok();
	}

	// 답글 작성
	@PostMapping("/{feedId}/comments/{commentId}/replies")
	public ApiResponse<Void> createReply(
			@PathVariable Long feedId,
			@PathVariable Long commentId) {
		log.info("[createReply] feedId={}, commentId={}", feedId, commentId);
		feedService.createReply(feedId, commentId);
		return ApiResponse.ok();
	}

	// 답글 수정
	@PatchMapping("/{feedId}/comments/{commentId}/replies/{replyId}")
	public ApiResponse<Void> updateReply(
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@PathVariable Long replyId) {
		log.info("[updateReply] feedId={}, commentId={}, replyId={}", feedId, commentId, replyId);
		feedService.updateReply(feedId, commentId, replyId);
		return ApiResponse.ok();
	}

	// 답글 삭제
	@DeleteMapping("/{feedId}/comments/{commentId}/replies/{replyId}")
	public ApiResponse<Void> deleteReply(
			@PathVariable Long feedId,
			@PathVariable Long commentId,
			@PathVariable Long replyId) {
		log.info("[deleteReply] feedId={}, commentId={}, replyId={}", feedId, commentId, replyId);
		feedService.deleteReply(feedId, commentId, replyId);
		return ApiResponse.ok();
	}
}
