package me._hanho.ultary.domain.admin;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.bff.BffSecurityProperties;
import me._hanho.ultary.domain.ai.ModerationQueue;
import me._hanho.ultary.domain.ai.AiRequestLogMapper;
import me._hanho.ultary.domain.ai.model.AiRequestLog;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="ai.moderation.worker-enabled=false")
@AutoConfigureMockMvc
@Transactional
class AdminManagementIntegrationTest {
    @Autowired AdminManagementService management;
    @Autowired AdminService adminService;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired BffSecurityProperties bff;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ModerationQueue queue;
    @Autowired AiRequestLogMapper aiLogs;
    Long user,admin,feed,report;
    String nickname,token;

    @BeforeEach void setup() {
        nickname="audit_"+UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO ultary_user(nickname,name,email,phone) VALUES(?,'테스트회원','tester@example.com','01012345678')",nickname);user=id();
        jdbc.update("INSERT INTO ultary_admin(login_id,password,name) VALUES(?,'{noop}test','검증관리자')",nickname);admin=id();
        token=jwt.createAdminAccessToken(admin);
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'검증 게시글','PRIVATE')",user);feed=id();
        jdbc.update("INSERT INTO ultary_report(reporter_user_no,target_type,target_feed_id,reason) VALUES(?,'FEED',?,'SPAM')",user,feed);report=id();
    }

    @Test void usersAreSearchableWithMatchingCountsAndStatusFilters() {
        var page=management.users("@"+nickname,"ACTIVE",0,1);
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(row->{
            assertThat(row.getUserNo()).isEqualTo(user);
            assertThat(row.getFeedCount()).isEqualTo(1);
            assertThat(row.getReportCount()).isEqualTo(1);
        });
        assertThat(management.users(nickname,"SUSPENDED",0,1).totalCount()).isZero();
        assertThat(management.users(user.toString(),null,0,10).items()).extracting(x->x.getUserNo()).containsExactly(user);
        assertThat(management.users(nickname,null,1,1).items()).isEmpty();
        assertThatThrownBy(()->management.users(null,"BAD",0,10)).isInstanceOf(BusinessException.class);
    }

    @Test void petNameSearchSharesFiltersAndReturnsManagementOrderWithoutDuplicateUsers() {
        String marker=UUID.randomUUID().toString().substring(0,8);
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,2)",user,"late_"+marker,"나중"+marker);
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority) VALUES(?,?,?,1)",user,"first_"+marker,"먼저"+marker);
        jdbc.update("INSERT INTO ultary_pet(user_no,mention_id,name,priority,is_deleted) VALUES(?,?,?,0,1)",user,"deleted_"+marker,"삭제펫"+marker);
        var page=management.users(marker,"ACTIVE",0,1);
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(row->assertThat(row.getPetNames()).containsExactly("먼저"+marker,"나중"+marker));
        assertThat(management.users(marker,"SUSPENDED",0,10).totalCount()).isZero();
        assertThat(management.users("삭제펫"+marker,null,0,10).totalCount()).isZero();
        assertThat(management.users(marker,null,1,1).items()).isEmpty();
        assertThat(management.user(user).user().getPetNames()).containsExactly("먼저"+marker,"나중"+marker);
    }

    @Test void detailIncludesPrivateContentAndReportsButNeverCredentialsOrRawContacts() throws Exception {
        mvc.perform(get("/api/v1/admin/users/"+user).secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.maskedEmail").value("t***@example.com"))
            .andExpect(jsonPath("$.data.user.maskedPhone").value("***-****-5678"))
            .andExpect(jsonPath("$.data.user.password").doesNotExist()).andExpect(jsonPath("$.data.user.email").doesNotExist())
            .andExpect(jsonPath("$.data.user.phone").doesNotExist()).andExpect(jsonPath("$.data.feeds[0].feedId").value(feed))
            .andExpect(jsonPath("$.data.reports[0].reportId").value(report));
    }

    @Test void normalUserAndAnonymousRequestsCannotReadOrChangeAdminData() throws Exception {
        for(String path:new String[]{"/users","/users/"+user,"/actions"}) {
            mvc.perform(get("/api/v1/admin"+path).secure(true).header("X-BFF-Key",bff.getSharedKey())).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/admin"+path).secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+jwt.createUserAccessToken(user))).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/admin/users/"+user+"/suspend").secure(true).header("X-BFF-Key",bff.getSharedKey())
            .header("Authorization","Bearer "+jwt.createUserAccessToken(user)).contentType("application/json").content("{\"reason\":\"test\"}"))
            .andExpect(status().isUnauthorized());
        assertThat(management.actions(nickname,null,0,10).totalCount()).isZero();
    }

    @Test void suspensionAndReleaseWriteActorReasonAndBothStatuses() throws Exception {
        changeUser("suspend","반복 광고");
        changeUser("unsuspend","조치 완료");
        var logs=management.actions(nickname,"USER",0,10);
        assertThat(logs.totalCount()).isEqualTo(2);
        assertThat(logs.items().get(0).getAction()).isEqualTo("USER_UNSUSPENDED");
        assertThat(logs.items().get(0).getBeforeStatus()).isEqualTo("SUSPENDED");
        assertThat(logs.items().get(0).getAfterStatus()).isEqualTo("ACTIVE");
        assertThat(logs.items().get(1).getReason()).isEqualTo("반복 광고");
        assertThat(logs.items().get(1).getAdminLoginId()).isEqualTo(nickname);
        assertThat(logs.items().get(1).getTargetLabel()).isEqualTo("@"+nickname);
        assertThat(logs.items()).allSatisfy(row->assertThat(row.isSample()).isFalse());
    }

    @Test void historicalSamplesAreIdentifiedAndSortedByProcessingDateNotInsertionOrder() throws Exception {
        for(String date:new String[]{"2026-10-20 12:00:00","2026-10-10 12:00:00"}) {
            jdbc.update("INSERT INTO ultary_admin_action_log(admin_no,admin_login_id,action,target_type,target_id,is_sample,created_at) VALUES(?,?,'TAG_APPROVED','TAG',?,1,?)",admin,nickname,feed,date);
        }
        var rows=management.actions(nickname,"TAG",0,10).items();
        assertThat(rows).hasSize(2).allSatisfy(row->assertThat(row.isSample()).isTrue());
        assertThat(rows.get(0).getCreatedAt()).isAfter(rows.get(1).getCreatedAt());
        mvc.perform(get("/api/v1/admin/actions").param("q",nickname).secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].sample").value(true));
    }

    @Test void closedAccountCannotBeSuspendedAndFailureDoesNotWriteCompletedAction() throws Exception {
        jdbc.update("UPDATE ultary_user SET withdrawal_status='WITHDRAWN' WHERE user_no=?",user);
        mvc.perform(post("/api/v1/admin/users/"+user+"/suspend").secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_admin_action_log WHERE admin_no=?",Integer.class,admin)).isZero();
    }

    @Test void tagAndReportChangesAreAuditedFromExistingEndpoints() throws Exception {
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,profile_status,created_by_user_no) VALUES(?,'COMMERCIAL','PENDING',?)",nickname,user);Long tag=id();
        jdbc.update("INSERT INTO ultary_tag_profile_request(tag_id,requester_user_no,title) VALUES(?,?,'test')",tag,user);Long request=id();
        for(String action:new String[]{"approve","pending"}) mvc.perform(post("/api/v1/admin/tags/"+tag+"/"+action).param("requestId",request.toString())
            .secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/reports/"+report+"/confirm").secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ultary_admin_action_log WHERE admin_no=?",Integer.class,admin)).isEqualTo(3);
        assertThat(management.actions(nickname,"TAG",0,10).items()).extracting(x->x.getAfterStatus()).containsExactly("PENDING","APPROVED");
        assertThat(management.actions(report.toString(),"REPORT",0,10).items()).anySatisfy(row->assertThat(row.getAfterStatus()).isEqualTo("CONFIRMED"));
    }

    @Test void aiReviewAlsoUsesTheSameAuditStream() throws Exception {
        queue.enqueue("FEED",feed,feed,user,null,"검증 게시글",true);
        Long job=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE user_no=?",Long.class,user);
        var log=new AiRequestLog();log.setUserNo(user);log.setFeatureType("FEED_CAPTION");log.setStatus("FAILED");log.setModerationJobId(job);log.setTargetType("FEED");log.setTargetId(feed);log.setTargetFeedId(feed);aiLogs.insert(log);
        mvc.perform(post("/api/v1/admin/ai/logs/"+log.getAiRequestId()+"/review").secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token)
            .contentType("application/json").content("{\"status\":\"KEPT\"}")).andExpect(status().isOk());
        assertThat(management.actions(admin.toString(),"AI_REVIEW",0,10).items()).anySatisfy(row->{
            assertThat(row.getBeforeStatus()).isEqualTo("PENDING");assertThat(row.getAfterStatus()).isEqualTo("KEPT");
        });
    }

    @Test void auditInsertFailureRollsBackTheUserChangeAtItsTransactionBoundary() {
        var tx=new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        assertThatThrownBy(()->tx.execute(status->management.execute(Long.MAX_VALUE,"USER_SUSPENDED","USER",user,null,"test",()->adminService.suspendUser(user))))
            .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT withdrawal_status FROM ultary_user WHERE user_no=?",String.class,user)).isEqualTo("ACTIVE");
    }

    @Test void memberProfileCannotReadAnotherMembersFile() {
        jdbc.update("INSERT INTO ultary_file(store_name,file_path) VALUES('test.png','images/test.png')");Long file=id();
        assertThatThrownBy(()->management.assertProfileFile(user,file)).isInstanceOf(BusinessException.class);
    }
    private void changeUser(String action,String reason) throws Exception {
        mvc.perform(post("/api/v1/admin/users/"+user+"/"+action).secure(true).header("X-BFF-Key",bff.getSharedKey()).header("Authorization","Bearer "+token)
            .contentType("application/json").content("{\"reason\":\""+reason+"\"}")).andExpect(status().isOk());
    }
    private Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
}
