package me._hanho.ultary.domain.test;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.response.ApiResponse;
import me._hanho.ultary.domain.test.dto.request.PasswordEncodeRequest;
import me._hanho.ultary.domain.test.dto.response.PasswordEncodeResponse;

/**
 * 로컬 전용 테스트 API.
 * 평문 비밀번호 → BCrypt 등 인코딩 결과 (DB 시드/수동 INSERT용).
 */
@Slf4j
@Profile("local")
@Validated
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

	private final PasswordEncoder passwordEncoder;

	@PostMapping("/password/encode")
	public ApiResponse<PasswordEncodeResponse> encodePassword(
			@Valid @RequestBody PasswordEncodeRequest request) {
		log.info("[encodePassword]");
		String encoded = passwordEncoder.encode(request.getPassword());
		return ApiResponse.ok(PasswordEncodeResponse.builder()
				.encodedPassword(encoded)
				.build());
	}
}
