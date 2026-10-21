package me._hanho.ultary.domain.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.Filter;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.admin.AdminAuthService;
import me._hanho.ultary.domain.admin.dto.request.AdminLoginRequest;
import me._hanho.ultary.domain.auth.dto.request.LoginRequest;
import me._hanho.ultary.domain.auth.dto.request.RefreshTokenRequest;
import me._hanho.ultary.domain.auth.dto.response.TokenResponse;
import me._hanho.ultary.domain.auth.model.Token;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.jwt.TokenOwnerType;

/** 실제 JWT·MariaDB·보안 필터로 기기별 해제를 확인한다. 모든 픽스처는 롤백한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"ai.moderation.worker-enabled=false", "security.bff.shared-key=integration-test-bff-key-not-for-production"})
@Transactional
class AuthSessionIntegrationTest {

	@Autowired AuthService auth;
	@Autowired AdminAuthService adminAuth;
	@Autowired TokenMapper tokens;
	@Autowired JwtTokenProvider jwt;
	@Autowired JdbcTemplate jdbc;
	@Autowired PasswordEncoder passwords;
	@Autowired WebApplicationContext context;
	@Autowired @Qualifier("springSecurityFilterChain") Filter security;
	@Autowired me._hanho.ultary.security.bff.BffSecurityProperties bff;
	MockMvc mvc;
	Long userNo, adminNo;
	String email, adminLogin;
	final String password = "session-test-password";

	@BeforeEach void fixture() {
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security)
				.defaultRequest(post("/").header("X-BFF-Key", bff.getSharedKey())).build();
		String suffix = UUID.randomUUID().toString().substring(0, 12);
		email = "session-" + suffix + "@test.invalid";
		jdbc.update("INSERT INTO ultary_user(nickname,email,password) VALUES(?,?,?)",
				"session-" + suffix, email, passwords.encode(password));
		userNo = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		adminLogin = "session-" + suffix;
		jdbc.update("INSERT INTO ultary_admin(login_id,password) VALUES(?,?)", adminLogin, passwords.encode(password));
		adminNo = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	@Test void separateLoginsWithSameIpAndAgentRemainSeparateAfterRefreshAndLogout() throws Exception {
		TokenResponse first = userLogin();
		TokenResponse second = userLogin();
		Token firstRow = tokens.findValidUserRefreshToken(userNo, first.getRefreshToken());
		Token secondRow = tokens.findValidUserRefreshToken(userNo, second.getRefreshToken());
		assertThat(first.getRefreshToken()).isNotEqualTo(second.getRefreshToken());
		assertThat(firstRow.getSessionId()).isNotBlank().isNotEqualTo(secondRow.getSessionId());
		assertThat(firstRow.getConnectIp()).isEqualTo("203.0.113.25");
		assertThat(firstRow.getConnectAgent()).isEqualTo("browser-session-test");
		assertThat(firstRow.getLoggedInAt()).isNotNull();

		var changedConnection = connection();
		changedConnection.removeHeader("X-Forwarded-For");
		changedConnection.addHeader("X-Forwarded-For", "198.51.100.20");
		TokenResponse rotated = auth.refresh(refresh(first), changedConnection);
		Token rotatedRow = tokens.findValidUserRefreshToken(userNo, rotated.getRefreshToken());
		assertThat(rotatedRow.getSessionId()).isEqualTo(firstRow.getSessionId());
		assertThat(rotatedRow.getLoggedInAt()).isEqualTo(firstRow.getLoggedInAt());
		assertThat(rotatedRow.getConnectIp()).isEqualTo(firstRow.getConnectIp());
		assertThat(rotatedRow.getConnectAgent()).isEqualTo(firstRow.getConnectAgent());
		assertThat(jwt.parseRefreshToken(rotated.getRefreshToken()).get("sessionId", String.class))
				.isEqualTo(firstRow.getSessionId());
		assertThat(tokens.findValidUserRefreshToken(userNo, first.getRefreshToken())).isNull();

		// Access 쿠키/헤더 없이 Refresh Token 자체로 현재 세션을 해제한다.
		logout("/api/v1/auth/logout", rotated.getRefreshToken());
		assertThat(tokens.findValidUserRefreshToken(userNo, rotated.getRefreshToken())).isNull();
		assertThat(tokens.findValidUserRefreshToken(userNo, second.getRefreshToken())).isNotNull();
		assertThat(auth.refresh(refresh(second), connection()).getAccessToken()).isNotBlank();
		logout("/api/v1/auth/logout", rotated.getRefreshToken()); // 재전송도 다른 세션에 영향 없음
	}

	@Test void adminLogoutOnlyRevokesCurrentAdminSession() throws Exception {
		TokenResponse first = adminLogin();
		TokenResponse second = adminLogin();
		Token before = tokens.findValidAdminRefreshToken(adminNo, first.getRefreshToken());
		TokenResponse rotated = adminAuth.refresh(refresh(first), connection());
		Token after = tokens.findValidAdminRefreshToken(adminNo, rotated.getRefreshToken());
		assertThat(after.getSessionId()).isEqualTo(before.getSessionId());
		assertThat(after.getLoggedInAt()).isEqualTo(before.getLoggedInAt());
		assertThat(after.getConnectAgent()).isEqualTo("browser-session-test");
		logout("/api/v1/admin/auth/logout", rotated.getRefreshToken());
		assertThat(tokens.findValidAdminRefreshToken(adminNo, rotated.getRefreshToken())).isNull();
		assertThat(tokens.findValidAdminRefreshToken(adminNo, second.getRefreshToken())).isNotNull();
	}

	@Test void invalidOrWrongOwnerTokensCannotLogOutOtherSessions() throws Exception {
		TokenResponse user = userLogin();
		TokenResponse admin = adminLogin();
		mvc.perform(post("/api/v1/auth/logout").contentType("application/json")
				.content("{\"refreshToken\":\"" + admin.getRefreshToken() + "\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/admin/auth/logout").contentType("application/json")
				.content("{\"refreshToken\":\"" + user.getRefreshToken() + "\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/auth/logout").contentType("application/json")
				.content("{\"refreshToken\":\"invalid\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/auth/logout").contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest());
		assertThat(tokens.findValidUserRefreshToken(userNo, user.getRefreshToken())).isNotNull();
		assertThat(tokens.findValidAdminRefreshToken(adminNo, admin.getRefreshToken())).isNotNull();
	}

	@Test void legacyTokensAreUpgradedOnRefreshAndKeepOriginalLoginInformation() {
		Token legacy = legacyToken();
		TokenResponse response = auth.refresh(refresh(legacy.getRefreshToken()), connection());
		Token upgraded = tokens.findValidUserRefreshToken(userNo, response.getRefreshToken());
		assertThat(upgraded.getSessionId()).isNotBlank();
		assertThat(upgraded.getLoggedInAt()).isNotNull();
		assertThat(upgraded.getConnectIp()).isEqualTo("192.0.2.10");
		assertThat(upgraded.getConnectAgent()).isEqualTo("legacy-browser");
	}

	@Test void legacyLogoutOnlyRevokesItsOwnTokenAndJwtIdsAreUnique() {
		Token legacy = legacyToken();
		TokenResponse another = userLogin();
		auth.logout(refresh(legacy.getRefreshToken()));
		assertThat(tokens.findValidUserRefreshToken(userNo, legacy.getRefreshToken())).isNull();
		assertThat(tokens.findValidUserRefreshToken(userNo, another.getRefreshToken())).isNotNull();
		assertThat(jwt.parseRefreshToken(legacy.getRefreshToken()).getId()).isNotBlank()
				.isNotEqualTo(jwt.parseRefreshToken(another.getRefreshToken()).getId());
		assertThat(another.getRefreshToken().length()).isLessThanOrEqualTo(500);
		assertThatThrownBy(() -> auth.refresh(refresh(legacy.getRefreshToken()), connection()))
				.isInstanceOf(BusinessException.class);
	}

	Token legacyToken() {
		Token row = new Token();
		row.setOwnerType("USER"); row.setUserNo(userNo);
		row.setRefreshToken(jwt.createRefreshToken(userNo, TokenOwnerType.USER));
		row.setConnectIp("192.0.2.10"); row.setConnectAgent("legacy-browser");
		row.setExpiresAt(LocalDateTime.now().plusDays(1));
		tokens.insert(row);
		return row;
	}

	TokenResponse userLogin() {
		var request = new LoginRequest(); request.setEmail(email); request.setPassword(password);
		return auth.login(request, connection());
	}

	TokenResponse adminLogin() {
		var request = new AdminLoginRequest(); request.setLoginId(adminLogin); request.setPassword(password);
		return adminAuth.login(request, connection());
	}

	MockHttpServletRequest connection() {
		var request = new MockHttpServletRequest();
		request.addHeader("X-Forwarded-For", "203.0.113.25, 127.0.0.1");
		request.addHeader("User-Agent", "browser-session-test");
		return request;
	}

	RefreshTokenRequest refresh(TokenResponse response) { return refresh(response.getRefreshToken()); }
	RefreshTokenRequest refresh(String value) { var request = new RefreshTokenRequest(); request.setRefreshToken(value); return request; }
	void logout(String path, String token) throws Exception {
		mvc.perform(post(path).contentType("application/json").content("{\"refreshToken\":\"" + token + "\"}"))
				.andExpect(status().isOk());
	}
}
