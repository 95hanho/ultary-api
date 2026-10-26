package me._hanho.ultary.domain.seo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import me._hanho.ultary.common.response.ApiResponse;
@RestController @RequiredArgsConstructor
public class SeoController {
 private final SeoService service;
 @GetMapping("/api/v1/public/seo/sitemap")
 public ResponseEntity<ApiResponse<SeoService.Page>> sitemap(
   @RequestParam(defaultValue="PROFILE") String type,
   @RequestParam(defaultValue="0") Long cursorId,
   @RequestParam(defaultValue="200") int limit) {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
   .body(ApiResponse.ok(service.list(type,cursorId,limit),"공개 검색 목록 조회 성공"));
 }
}
