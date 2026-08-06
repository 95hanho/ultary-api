package me._hanho.ultary.domain.tag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.tag.service.TagService;

/** 경로 원본: springEndpoints.tags / api-memo.md §6 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

	private final TagService tagService;

	// 태그 등록
	@PostMapping
	public ApiResponse<Void> create() {
		log.info("[create]");
		tagService.create();
		return ApiResponse.ok();
	}

	// 태그 검색
	@GetMapping("/search")
	public ApiResponse<Void> search(@RequestParam(required = false) String q) {
		log.info("[search] q={}", q);
		tagService.search(q);
		return ApiResponse.ok();
	}

	// 내용 입력 시 태그 추천
	@GetMapping("/recommend")
	public ApiResponse<Void> recommend(@RequestParam(required = false) String q) {
		log.info("[recommend] q={}", q);
		tagService.recommend(q);
		return ApiResponse.ok();
	}

	// 태그 정보 조회 (호버·클릭)
	@GetMapping("/{tagId}")
	public ApiResponse<Void> detail(@PathVariable Long tagId) {
		log.info("[detail] tagId={}", tagId);
		tagService.getDetail(tagId);
		return ApiResponse.ok();
	}
}
