package me._hanho.ultary.domain.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.admin.AdminAuthService;
import me._hanho.ultary.domain.admin.dto.request.AdminLoginRequest;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;

/** 별도 스레드/DB 트랜잭션의 실제 경쟁을 검증한다. 생성한 테스트 계정만 정리한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"ai.moderation.worker-enabled=false", "security.bff.shared-key=integration-test-bff-key-not-for-production"})
class TokenRotationConcurrencyIntegrationTest {

	@Autowired AuthService auth;
	@Autowired AdminAuthService admin;
	@Autowired JdbcTemplate jdbc;
	@Autowired PasswordEncoder passwords;
	Long userNo, adminNo;
	String email, loginId;
	final String password = "rotation-test-password";

	@BeforeEach void fixture() {
		String suffix = UUID.randomUUID().toString().substring(0, 12);
		email = "rotate-" + suffix + "@test.invalid";
		jdbc.update("INSERT INTO ultary_user(nickname,email,password) VALUES(?,?,?)", "rotate-" + suffix, email, passwords.encode(password));
		userNo = jdbc.queryForObject("SELECT user_no FROM ultary_user WHERE email=?", Long.class, email);
		loginId = "rotate-" + suffix;
		jdbc.update("INSERT INTO ultary_admin(login_id,password) VALUES(?,?)", loginId, passwords.encode(password));
		adminNo = jdbc.queryForObject("SELECT admin_no FROM ultary_admin WHERE login_id=?", Long.class, loginId);
	}

	@AfterEach void cleanup() {
		if (userNo != null) {
			jdbc.update("DELETE FROM ultary_token WHERE owner_type='USER' AND user_no=?", userNo);
			jdbc.update("DELETE FROM ultary_user WHERE user_no=?", userNo);
		}
		if (adminNo != null) {
			jdbc.update("DELETE FROM ultary_token WHERE owner_type='ADMIN' AND admin_no=?", adminNo);
			jdbc.update("DELETE FROM ultary_admin WHERE admin_no=?", adminNo);
		}
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void simultaneousRefreshesCreateOnlyOneSuccessor(boolean isAdmin) throws Exception {
		TokenResponse original = login(isAdmin);
		var start = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(2);
		try {
			var first = pool.submit(() -> { start.await(); return refresh(isAdmin, original); });
			var second = pool.submit(() -> { start.await(); return refresh(isAdmin, original); });
			start.countDown();
			TokenResponse a = first.get(15, TimeUnit.SECONDS);
			TokenResponse b = second.get(15, TimeUnit.SECONDS);
			assertThat(a.getRefreshToken()).isEqualTo(b.getRefreshToken()).isNotEqualTo(original.getRefreshToken());
			assertThat(count(isAdmin, "is_revoked=0")).isEqualTo(1);
			assertThat(count(isAdmin, "1=1")).isEqualTo(2);
			assertThat(refresh(isAdmin, original).getRefreshToken()).isEqualTo(a.getRefreshToken());
			assertThat(count(isAdmin, "1=1")).isEqualTo(2);
		} finally {
			pool.shutdownNow();
			assertThat(pool.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
		}
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void retryOutsideGraceWindowCannotRecover(boolean isAdmin) {
		TokenResponse original = login(isAdmin);
		refresh(isAdmin, original);
		jdbc.update("UPDATE ultary_token SET rotated_at=DATE_SUB(NOW(), INTERVAL 30 SECOND) WHERE refresh_token=?", original.getRefreshToken());
		assertThatThrownBy(() -> refresh(isAdmin, original)).isInstanceOf(BusinessException.class);
		assertThat(count(isAdmin, "is_revoked=0")).isEqualTo(1);
	}

	@ParameterizedTest @ValueSource(booleans = {false, true})
	void logoutUsingRecentlyRotatedTokenPreventsResurrectionButKeepsOtherDevice(boolean isAdmin) {
		TokenResponse original = login(isAdmin);
		TokenResponse otherDevice = login(isAdmin);
		TokenResponse rotated = refresh(isAdmin, original);
		logout(isAdmin, original);
		logout(isAdmin, original); // idempotent even when the successor was already revoked
		assertThatThrownBy(() -> refresh(isAdmin, original)).isInstanceOf(BusinessException.class);
		assertThatThrownBy(() -> refresh(isAdmin, rotated)).isInstanceOf(BusinessException.class);
		assertThat(refresh(isAdmin, otherDevice).getAccessToken()).isNotBlank();
		assertThat(count(isAdmin, "is_revoked=0")).isEqualTo(1);
	}

	@Test void delayedRequestCanFollowSeveralRotationsWithinSameSession() {
		TokenResponse original = login(false);
		TokenResponse first = refresh(false, original);
		TokenResponse latest = refresh(false, first);
		assertThat(refresh(false, original).getRefreshToken()).isEqualTo(latest.getRefreshToken());
		assertThat(count(false, "1=1")).isEqualTo(3);
	}

	@Test void replacementFromAnotherSessionCannotBeUsedForRecovery() {
		TokenResponse original = login(false);
		refresh(false, original);
		TokenResponse otherDevice = login(false);
		Long otherId = jdbc.queryForObject("SELECT token_id FROM ultary_token WHERE refresh_token=?", Long.class, otherDevice.getRefreshToken());
		jdbc.update("UPDATE ultary_token SET replaced_by_token_id=? WHERE refresh_token=?", otherId, original.getRefreshToken());
		assertThatThrownBy(() -> refresh(false, original)).isInstanceOf(BusinessException.class);
	}

	int count(boolean isAdmin, String condition) {
		String column = isAdmin ? "admin_no" : "user_no";
		return jdbc.queryForObject("SELECT COUNT(*) FROM ultary_token WHERE " + column + "=? AND " + condition,
				Integer.class, isAdmin ? adminNo : userNo);
	}

	TokenResponse login(boolean isAdmin) {
		if (isAdmin) {
			var request = new AdminLoginRequest(); request.setLoginId(loginId); request.setPassword(password);
			return admin.login(request, connection());
		}
		var request = new LoginRequest(); request.setEmail(email); request.setPassword(password);
		return auth.login(request, connection());
	}
	TokenResponse refresh(boolean isAdmin, TokenResponse token) {
		return isAdmin ? admin.refresh(request(token), connection()) : auth.refresh(request(token), connection());
	}
	void logout(boolean isAdmin, TokenResponse token) {
		if (isAdmin) admin.logout(request(token)); else auth.logout(request(token));
	}
	RefreshTokenRequest request(TokenResponse token) {
		var request = new RefreshTokenRequest(); request.setRefreshToken(token.getRefreshToken()); return request;
	}
	MockHttpServletRequest connection() {
		var request = new MockHttpServletRequest(); request.addHeader("User-Agent", "rotation-integration-test");
		request.setRemoteAddr("192.0.2.18"); return request;
	}
}
