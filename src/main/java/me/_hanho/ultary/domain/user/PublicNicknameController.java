package me._hanho.ultary.domain.user;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.user.dto.response.NicknameResolveResponse;

/**
 * 공개 닉네임태그 해석.
 * 현재 닉네임이 없으면 변경 후 7일 이내 직전 닉네임으로 현재 닉네임을 돌려준다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicNicknameController {

	private final NicknameLookupService nicknameLookupService;

	@GetMapping("/api/v1/public/nicknames/{nickname}")
	public ResponseEntity<ApiResponse<NicknameResolveResponse>> resolve(@PathVariable String nickname) {
		log.info("[resolveNickname] nickname={}", nickname);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(nicknameLookupService.resolve(nickname), "닉네임태그 해석 성공"));
	}
}
