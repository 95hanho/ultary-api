package me._hanho.ultary.domain.myultary;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.settings.PrivacyService;
import me._hanho.ultary.domain.settings.dto.request.UpdatePrivacyRequest;
import me._hanho.ultary.security.principal.UserPrincipal;
import me._hanho.ultary.security.bff.BffSecurityProperties;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="ai.moderation.worker-enabled=false")
@Transactional
class PublicUltaryIntegrationTest {
    @Autowired MyUltaryService service;
    @Autowired FileService files;
    @Autowired PrivacyService privacy;
    @Autowired JdbcTemplate jdbc;
    @Autowired SqlSessionTemplate session;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired BffSecurityProperties bff;
    MockMvc mvc;
    Long owner, pet, profileFile, publicFile, privateFile, neighborFile, unusedFile, publicFeed;
    String nickname;

    @BeforeEach void setup() {
        nickname="guest"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        jdbc.update("INSERT INTO ultary_user(nickname,email,phone,bio) VALUES(?,?,?,'public bio')", nickname,nickname+"@example.test","010"+System.nanoTime()%100000000);
        owner=id(); profileFile=file(); publicFile=file(); privateFile=file(); neighborFile=file(); unusedFile=file();
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority,profile_file_id) VALUES(?,?,?,1,?)",owner,nickname+"pet","guest pet",profileFile);
        pet=id(); publicFeed=feed("PUBLIC",publicFile);
        Long hidden=feed("PRIVATE",privateFile); feed("NEIGHBORS",neighborFile);
        jdbc.update("INSERT INTO ultary_feed_pin(user_no,feed_id) VALUES(?,?),(?,?)",owner,publicFeed,owner,hidden);
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    @Test void anonymousPublicProfileShowsOnlyPublicPostsAndNoMemberFields() throws Exception {
        var view=service.getPublicProfile(nickname);
        assertThat(view.feedCount()).isEqualTo(1);
        assertThat(view.feeds()).extracting(x->x.getFeedId()).containsExactly(publicFeed);
        assertThat(view.pinnedFeeds()).extracting(x->x.getFeedId()).containsExactly(publicFeed);
        assertThat(view.pets()).singleElement().satisfies(p->assertThat(p.mentionId()).isEqualTo(nickname+"pet"));
        mvc.perform(get("/api/v1/public/ultaries/{nickname}",nickname).header("X-BFF-Key",bff.getSharedKey()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.email").doesNotExist()).andExpect(jsonPath("$.data.phone").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist()).andExpect(jsonPath("$.data.regionSido").doesNotExist());
    }

    @Test void privateAccountsAndTheirImagesBecomeUnavailableImmediately() throws Exception {
        service.getPublicProfile(nickname);
        var settings=new UpdatePrivacyRequest(); settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(owner,"owner"),settings);
        assertThatThrownBy(()->service.getPublicProfile(nickname)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->files.assertPublicContent(profileFile)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->files.assertPublicContent(publicFile)).isInstanceOf(BusinessException.class);
        mvc.perform(get("/api/v1/public/ultaries/{nickname}",nickname).header("X-BFF-Key",bff.getSharedKey()))
            .andExpect(status().isNotFound());
    }

    @Test void anonymousFilesRequireAnActivePublicReference() throws Exception {
        files.assertPublicContent(profileFile); files.assertPublicContent(publicFile);
        for(Long id:new Long[]{privateFile,neighborFile,unusedFile})
            assertThatThrownBy(()->files.assertPublicContent(id)).isInstanceOf(BusinessException.class);
        mvc.perform(get("/api/v1/files/{fileId}/content",unusedFile).header("X-BFF-Key",bff.getSharedKey()))
            .andExpect(status().isNotFound());
        jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?",publicFeed); session.clearCache();
        assertThatThrownBy(()->files.assertPublicContent(publicFile)).isInstanceOf(BusinessException.class);
    }

    @Test void publicProfileStillRequiresTheBffKeyAndDoesNotOpenOtherMemberApis() throws Exception {
        mvc.perform(get("/api/v1/public/ultaries/{nickname}",nickname)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/{userNo}/pets",owner).header("X-BFF-Key",bff.getSharedKey()))
            .andExpect(status().isUnauthorized());
    }

    @Test void taggedPostsRespectTheActualAuthorsVisibilityAndPrivateAccount() {
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)",nickname+"other");
        Long other=id(), visible=null;
        for (String visibility:new String[]{"PUBLIC","PRIVATE","NEIGHBORS"}) {
            jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,?)",other,visibility);
            Long feed=id(); if (visibility.equals("PUBLIC")) visible=feed;
            jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)",feed,publicFile);
            jdbc.update("INSERT INTO ultary_feed_media_mention(feed_media_id,pet_id,pos_x,pos_y,added_by_user_no) VALUES(?,?,10,20,?)",id(),pet,other);
        }
        session.clearCache();
        assertThat(service.getPublicProfile(nickname).taggedFeeds()).extracting(x->x.getFeedId()).containsExactly(visible);
        var settings=new UpdatePrivacyRequest();settings.setPrivateAccount(true);
        privacy.update(new UserPrincipal(other,"other"),settings);
        assertThat(service.getPublicProfile(nickname).taggedFeeds()).isEmpty();
    }

    Long file() { jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('guest.jpg','images/guest.jpg','image/jpeg','jpg','OWNED',?)",owner);return id(); }
    Long feed(String visibility,Long file) { jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,?)",owner,visibility);Long feed=id();jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)",feed,file);return feed; }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class); }
}
