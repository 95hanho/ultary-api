package me._hanho.ultary.domain.auth;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.auth.dto.request.SocialLoginRequest;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "ai.moderation.worker-enabled=false")
@Transactional
class SocialWithdrawalIntegrationTest {
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    Long owner, target;
    String providerId;

    @BeforeEach void fixture() {
        providerId = "withdrawal-" + UUID.randomUUID();
        owner = user();
        target = user();
    }

    @ParameterizedTest @ValueSource(strings = {"GOOGLE", "KAKAO"})
    void withdrawalReleasesSocialIdentityAndRevokesSessions(String provider) {
        link(owner, provider);
        auth.socialLogin(request(provider), new MockHttpServletRequest());

        auth.withdraw(principal(owner));

        assertThat(jdbc.queryForObject("SELECT withdrawal_status FROM ultary_user WHERE user_no=?", String.class, owner)).isEqualTo("WITHDRAWN");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_user_social WHERE user_no=?", Integer.class, owner)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_token WHERE user_no=? AND is_revoked=0", Integer.class, owner)).isZero();
        auth.linkSocial(principal(target), request(provider));
        assertOwner(provider, target);
    }

    @ParameterizedTest @ValueSource(strings = {"GOOGLE", "KAKAO"})
    void legacyWithdrawnIdentityCanBeLinkedWithoutRestoringOldAccount(String provider) {
        link(owner, provider);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='WITHDRAWN' WHERE user_no=?", owner);

        auth.linkSocial(principal(target), request(provider));

        assertOwner(provider, target);
        assertThat(jdbc.queryForObject("SELECT withdrawal_status FROM ultary_user WHERE user_no=?", String.class, owner)).isEqualTo("WITHDRAWN");
    }

    @ParameterizedTest @ValueSource(strings = {"GOOGLE", "KAKAO"})
    void legacyWithdrawnIdentityCanCreateFreshSocialAccount(String provider) {
        link(owner, provider);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='WITHDRAWN' WHERE user_no=?", owner);

        var result = auth.socialLogin(request(provider), new MockHttpServletRequest());

        assertThat(result.getAccessToken()).isNotBlank();
        Long newOwner = jdbc.queryForObject("SELECT user_no FROM ultary_user_social WHERE provider=? AND provider_user_id=?", Long.class, provider, providerId);
        assertThat(newOwner).isNotEqualTo(owner).isNotEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT withdrawal_status FROM ultary_user WHERE user_no=?", String.class, newOwner)).isEqualTo("ACTIVE");
    }

    @ParameterizedTest @ValueSource(strings = {"GOOGLE", "KAKAO"})
    void activeIdentityCannotBeTakenOver(String provider) {
        link(owner, provider);

        assertThatThrownBy(() -> auth.linkSocial(principal(target), request(provider)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_ACCOUNT_IN_USE));

        assertOwner(provider, owner);
    }

    @ParameterizedTest @ValueSource(strings = {"GOOGLE", "KAKAO"})
    void suspendedIdentityCannotBeTakenOverOrUsedToRejoin(String provider) {
        link(owner, provider);
        jdbc.update("UPDATE ultary_user SET withdrawal_status='SUSPENDED' WHERE user_no=?", owner);

        assertThatThrownBy(() -> auth.linkSocial(principal(target), request(provider)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SOCIAL_ACCOUNT_IN_USE));
        assertThatThrownBy(() -> auth.socialLogin(request(provider), new MockHttpServletRequest()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_SUSPENDED));
        assertOwner(provider, owner);
    }

    private Long user() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", "social_" + UUID.randomUUID().toString().substring(0, 8));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void link(Long user, String provider) {
        jdbc.update("INSERT INTO ultary_user_social(user_no,provider,provider_user_id) VALUES(?,?,?)", user, provider, providerId);
    }

    private SocialLoginRequest request(String provider) {
        var request = new SocialLoginRequest();
        request.setProvider(provider);
        request.setProviderUserId(providerId);
        return request;
    }

    private UserPrincipal principal(Long user) { return new UserPrincipal(user, "socialtest"); }

    private void assertOwner(String provider, Long expected) {
        assertThat(jdbc.queryForObject("SELECT user_no FROM ultary_user_social WHERE provider=? AND provider_user_id=?", Long.class, provider, providerId)).isEqualTo(expected);
    }
}
