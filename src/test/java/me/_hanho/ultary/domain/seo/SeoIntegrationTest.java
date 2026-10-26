package me._hanho.ultary.domain.seo;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import me._hanho.ultary.common.exception.BusinessException;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "ai.moderation.worker-enabled=false","security.bff.shared-key=integration-test-bff-key-not-for-production"})
@Transactional
class SeoIntegrationTest {
 @Autowired JdbcTemplate jdbc;
 @Autowired SeoService service;
 @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
 @Autowired WebApplicationContext context;
 @Autowired @Qualifier("springSecurityFilterChain") Filter security;
 MockMvc mvc;
 long userCursor,feedCursor;
 Long owner;
 @BeforeEach void setup(){
  userCursor=jdbc.queryForObject("SELECT COALESCE(MAX(user_no),0) FROM ultary_user",Long.class);
  feedCursor=jdbc.queryForObject("SELECT COALESCE(MAX(feed_id),0) FROM ultary_feed",Long.class);
  owner=user("ACTIVE",false);
  mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
 }
 Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 Long user(String status,boolean hidden){
  jdbc.update("INSERT INTO ultary_user(nickname,withdrawal_status,email,phone) VALUES(?,?,?,?)",
   "seo"+UUID.randomUUID().toString().substring(0,12),status,UUID.randomUUID()+"@example.test",UUID.randomUUID().toString().substring(0,20));
  Long id=id();if(hidden)jdbc.update("INSERT INTO ultary_user_privacy(user_no,private_account) VALUES(?,1)",id);return id;
 }
 Long feed(Long user,String visibility,boolean deleted){
  jdbc.update("INSERT INTO ultary_feed(user_no,visibility,is_deleted,content) VALUES(?,?,?,'public caption')",user,visibility,deleted);return id();
 }
 @Test void profilesOnlyIncludeActivePublicAccounts(){
  user("ACTIVE",true);user("SUSPENDED",false);user("WITHDRAWN",false);user("REQUESTED",false);
  assertThat(service.list("PROFILE",userCursor,100).items()).extracting(SeoEntry::getId).containsExactly(owner);
 }
 @Test void feedsRequireActivePublicAuthorAndPublicUndeletedContent(){
  Long visible=feed(owner,"PUBLIC",false);
  feed(owner,"PRIVATE",false);feed(owner,"NEIGHBORS",false);feed(owner,"PUBLIC",true);
  feed(user("ACTIVE",true),"PUBLIC",false);feed(user("SUSPENDED",false),"PUBLIC",false);feed(user("WITHDRAWN",false),"PUBLIC",false);
  assertThat(service.list("FEED",feedCursor,100).items()).extracting(SeoEntry::getId).containsExactly(visible);
 }
 @Test void cursorPagesDoNotSkipOrRepeatEntries(){
  Long second=user("ACTIVE",false),third=user("ACTIVE",false);
  var first=service.list("PROFILE",userCursor,1);assertThat(first.items()).extracting(SeoEntry::getId).containsExactly(owner);assertThat(first.nextCursorId()).isEqualTo(owner);
  var next=service.list("PROFILE",first.nextCursorId(),1);assertThat(next.items()).extracting(SeoEntry::getId).containsExactly(second);
  var last=service.list("PROFILE",next.nextCursorId(),1);assertThat(last.items()).extracting(SeoEntry::getId).containsExactly(third);assertThat(last.nextCursorId()).isNull();
 }
 @Test void privacyAndDeletionChangesAreAppliedImmediately(){
  Long post=feed(owner,"PUBLIC",false);
  assertThat(service.list("FEED",feedCursor,100).items()).hasSize(1);
  jdbc.update("INSERT INTO ultary_user_privacy(user_no,private_account) VALUES(?,1)",owner);
  sqlSession.clearCache(); // Model the next HTTP request after the direct JDBC fixture change.
  assertThat(service.list("PROFILE",userCursor,100).items()).isEmpty();assertThat(service.list("FEED",feedCursor,100).items()).isEmpty();
  jdbc.update("UPDATE ultary_user_privacy SET private_account=0 WHERE user_no=?",owner);
  jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?",post);
  sqlSession.clearCache();
  assertThat(service.list("PROFILE",userCursor,100).items()).hasSize(1);assertThat(service.list("FEED",feedCursor,100).items()).isEmpty();
 }
 @Test void publicEndpointNeedsOnlyServerKeyAndNeverExposesContactFields()throws Exception{
  mvc.perform(get("/api/v1/public/seo/sitemap").header("X-BFF-Key","integration-test-bff-key-not-for-production")
   .param("type","PROFILE").param("cursorId",String.valueOf(userCursor)))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.data.items[0].id").value(owner.intValue()))
   .andExpect(jsonPath("$.data.items[0].email").doesNotExist()).andExpect(jsonPath("$.data.items[0].phone").doesNotExist());
  mvc.perform(get("/api/v1/public/seo/sitemap")).andExpect(status().isForbidden());
 }
 @Test void invalidTypeCursorAndLimitAreRejected(){
  assertThatThrownBy(()->service.list("ALL",0L,200)).isInstanceOf(BusinessException.class);
  assertThatThrownBy(()->service.list("FEED",-1L,200)).isInstanceOf(BusinessException.class);
  assertThatThrownBy(()->service.list("FEED",0L,1001)).isInstanceOf(BusinessException.class);
 }
}
