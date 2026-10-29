package me._hanho.ultary.domain.auth;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.auth.dto.request.*;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.pet.dto.request.CreatePetRequest;
import me._hanho.ultary.domain.myultary.UltaryLinkService;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class MultiAccountIntegrationTest {
    @Autowired AuthService auth;
    @Autowired PetService pets;
    @Autowired UltaryLinkService links;
    @Autowired me._hanho.ultary.domain.settings.PrivacyService privacy;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtTokenProvider jwt;
    @Autowired ObjectMapper json;
    String email, phone, password="Testpass1!";
    Long first, second, other;
    @BeforeEach void fixture() {
        email=UUID.randomUUID()+"@test.invalid";
        phone="010"+String.format("%08d",new java.security.SecureRandom().nextInt(100000000));
        first=user(password); second=user(password); other=user("Different1!");
    }
    @Test void loginChoicesRequirePasswordAndDoNotIssueTokensUntilSelected() {
        var request=login(password);
        var choices=auth.login(request,new MockHttpServletRequest());
        assertThat(choices.isSelectionRequired()).isTrue();
        assertThat(choices.getAccounts()).extracting(a->a.userNo()).containsExactly(first,second);
        assertThat(choices.getAccessToken()).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_token WHERE user_no IN (?,?)",Integer.class,first,second)).isZero();
        request.setUserNo(second);
        var selected=auth.login(request,new MockHttpServletRequest());
        assertThat(jwt.getSubjectAsLong(jwt.parseUserAccessToken(selected.getAccessToken()))).isEqualTo(second);
        request.setUserNo(other);
        assertThatThrownBy(()->auth.login(request,new MockHttpServletRequest())).isInstanceOf(BusinessException.class);
        var unique=auth.login(login("Different1!"),new MockHttpServletRequest());
        assertThat(unique.isSelectionRequired()).isFalse();
        assertThat(jwt.getSubjectAsLong(jwt.parseUserAccessToken(unique.getAccessToken()))).isEqualTo(other);
    }
    @Test void sharedPhoneRecoveryResetsOnlyTheSelectedAccount() {
        var request=new PasswordTokenRequest(); request.setPhone(phone);
        request.setPhoneAuthCompleteToken(jwt.createPhoneAuthCompleteToken(phone));
        var choices=auth.createPasswordToken(request);
        assertThat(choices.isSelectionRequired()).isTrue();
        assertThat(choices.getPasswordChangeToken()).isNull();
        request.setUserNo(second);
        var reset=new ChangePasswordRequest();
        reset.setPasswordChangeToken(auth.createPasswordToken(request).getPasswordChangeToken());
        reset.setNewPassword("Changed123!"); auth.changePassword(reset);
        assertThat(passwords.matches(password,storedPassword(first))).isTrue();
        assertThat(passwords.matches("Changed123!",storedPassword(second))).isTrue();
        request.setUserNo(-1L);
        assertThatThrownBy(()->auth.createPasswordToken(request)).isInstanceOf(BusinessException.class);
    }
    @Test void signupAllowsSharedContactButStillRequiresUniqueNickname() {
        var request=new SignupRequest(); request.setPhoneAuthCompleteToken(jwt.createPhoneAuthCompleteToken(phone));
        request.setEmail(email); request.setPassword(password);
        request.setNickname("s"+UUID.randomUUID().toString().replace("-","").replaceAll("[0-9]","a").substring(0,8));
        auth.signup(request);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_user WHERE phone=? AND email=?",Integer.class,phone,email)).isEqualTo(4);
        assertThatThrownBy(()->auth.signup(request)).isInstanceOf(BusinessException.class);
        var login=login(password); login.setEmail(null); login.setPhone(phone);
        assertThat(auth.login(login,new MockHttpServletRequest()).getAccounts()).hasSize(3);
    }
    @Test void petLimitAndNameOnlyResponsePreserveThreeDistinctIds() throws Exception {
        var request=new CreatePetRequest(); request.setName("초코");
        var principal=new UserPrincipal(first,"test");
        var pet=pets.create(principal,request); pets.create(principal,request); pets.create(principal,request);
        assertThat(pets.list(principal)).hasSize(3).extracting(p->p.getPetId()).doesNotHaveDuplicates();
        assertThat(json.writeValueAsString(pet)).doesNotContain("mentionId");
        assertThatThrownBy(()->pets.create(principal,request)).isInstanceOf(BusinessException.class);
    }
    @Test void linksArePublicSearchableIdempotentAndRemovableWhenTargetTurnsPrivate() {
        links.add(first,second); links.add(first,second);
        assertThat(links.list(first,null)).singleElement().satisfies(row->assertThat(row.userNo()).isEqualTo(second));
        var nick=jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?",String.class,second);
        assertThat(links.search(first,nick)).extracting(row->row.userNo()).containsExactly(second);
        var settings=new me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest(); settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(second,"test"),settings);
        assertThat(links.list(first,null)).isEmpty();
        assertThat(links.list(first,first)).singleElement().satisfies(row->assertThat(row.available()).isFalse());
        assertThatThrownBy(()->links.add(other,second)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->links.add(first,first)).isInstanceOf(BusinessException.class);
        links.delete(first,second); assertThat(links.list(first,first)).isEmpty();
    }
    @Test void blockedUsersCannotReadOrCreateLinksToTheBlockingAccount() {
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?)",second,first);
        assertThatThrownBy(()->links.add(first,second)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->links.list(second,first)).isInstanceOf(BusinessException.class);
    }
    @Test void linkedAccountSearchIncludesOrderedPetNamesAndHidesPrivateTargetDetails() {
        var request=new CreatePetRequest(); request.setName("초코");
        pets.create(new UserPrincipal(second,"test"),request);
        request.setName("달이"); pets.create(new UserPrincipal(second,"test"),request);
        assertThat(links.search(first,"초코")).filteredOn(row->row.userNo().equals(second)).singleElement().satisfies(row->{
            assertThat(row.userNo()).isEqualTo(second);
            assertThat(row.petNames()).containsExactly("초코","달이");
            assertThat(row.hasStory()).isFalse();
            assertThat(row.hasUnviewed()).isFalse();
        });
        links.add(first,second);
        assertThat(links.list(first,null)).singleElement().satisfies(row->assertThat(row.petNames()).containsExactly("초코","달이"));
        var settings=new me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest(); settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(second,"test"),settings);
        assertThat(links.list(first,first)).singleElement().satisfies(row->{
            assertThat(row.available()).isFalse();
            assertThat(row.petNames()).isEmpty();
            assertThat(row.profileFile()).isNull();
        });
    }
    String storedPassword(Long id){return jdbc.queryForObject("SELECT password FROM ultary_user WHERE user_no=?",String.class,id);}
    LoginRequest login(String value){var r=new LoginRequest();r.setEmail(email);r.setPassword(value);return r;}
    Long user(String value){jdbc.update("INSERT INTO ultary_user(nickname,email,phone,password) VALUES(?,?,?,?)","a"+UUID.randomUUID().toString().replace("-","").substring(0,8),email,phone,passwords.encode(value));return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
