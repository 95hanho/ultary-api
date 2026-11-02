package me._hanho.ultary.domain.test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.story.StoryMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.security.bff.BffSecurityProperties;
import me._hanho.ultary.security.jwt.JwtTokenProvider;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="ai.moderation.worker-enabled=false")
@AutoConfigureMockMvc
@Transactional
class ExperienceControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenProvider jwt;
    @Autowired BffSecurityProperties bff;
    Long actor,other;
    String token;

    @BeforeEach void setup() {
        actor=user(); other=user(); token=jwt.createUserAccessToken(actor);
    }

    @Test void productionRegistersOnlyTheTwoExperienceActions() {
        try(var context=new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.registerBean(StoryMapper.class,()->mock(StoryMapper.class));
            context.registerBean(UserMapper.class,()->mock(UserMapper.class));
            context.register(ExperienceController.class,TestController.class);
            context.refresh();
            assertThat(context.getBeansOfType(ExperienceController.class)).hasSize(1);
            assertThat(context.getBeansOfType(TestController.class)).isEmpty();
        }
    }

    @Test void anonymousAndDirectRequestsCannotRunResets() throws Exception {
        mvc.perform(delete("/api/v1/test/story-views").header("X-BFF-Key",bff.getSharedKey()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/test/nickname-cooldown").header("X-BFF-Key",bff.getSharedKey()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/test/story-views").header("Authorization","Bearer "+token))
                .andExpect(status().isForbidden());
    }

    @Test void storyResetDeletesOnlyAuthenticatedViewersRows() throws Exception {
        jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('experience.jpg','images/experience.jpg','image/jpeg','jpg','OWNED',?)",other);
        Long file=id();
        jdbc.update("INSERT INTO ultary_story(user_no,file_id,media_type,expires_at) VALUES(?,?,'IMAGE',DATE_ADD(NOW(),INTERVAL 1 DAY))",other,file);
        Long story=id();
        jdbc.update("INSERT INTO ultary_story_view(story_id,viewer_user_no) VALUES(?,?),(?,?)",story,actor,story,other);
        mvc.perform(delete("/api/v1/test/story-views").queryParam("userNo",other.toString())
                .header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.viewerUserNo").value(actor))
                .andExpect(jsonPath("$.data.deletedCount").value(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_story_view WHERE story_id=? AND viewer_user_no=?",Integer.class,story,actor)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_story_view WHERE story_id=? AND viewer_user_no=?",Integer.class,story,other)).isEqualTo(1);
    }

    @Test void nicknameResetUnlocksOnlyAuthenticatedUserWithoutChangingNickname() throws Exception {
        LocalDateTime before=jdbc.queryForObject("SELECT nickname_changed_at FROM ultary_user WHERE user_no=?",LocalDateTime.class,other);
        String nickname=jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?",String.class,actor);
        mvc.perform(post("/api/v1/test/nickname-cooldown").contentType("application/json")
                .content("{\"userNo\":"+other+"}").header("X-BFF-Key",bff.getSharedKey())
                .header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userNo").value(actor));
        assertThat(jdbc.queryForObject("SELECT nickname_changed_at FROM ultary_user WHERE user_no=?",LocalDateTime.class,actor))
                .isBefore(LocalDateTime.now().minusDays(30)).isAfter(LocalDateTime.now().minusDays(32));
        assertThat(jdbc.queryForObject("SELECT nickname_changed_at FROM ultary_user WHERE user_no=?",LocalDateTime.class,other)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?",String.class,actor)).isEqualTo(nickname);
    }

    Long user() {jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","demo_"+UUID.randomUUID().toString().substring(0,8));return id();}
    Long id() {return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
