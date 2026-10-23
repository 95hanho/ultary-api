package me._hanho.ultary.domain.tag;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import me._hanho.ultary.security.jwt.JwtTokenProvider;
import me._hanho.ultary.security.bff.BffSecurityProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ai.moderation.worker-enabled=false")
@AutoConfigureMockMvc
@Transactional
class TagReviewIntegrationTest {
    @Autowired TagService tags;
    @Autowired FeedService feeds;
    @Autowired NotificationService notifications;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired BffSecurityProperties bff;
    Long user, admin, tag, request, feed;
    String hashtag;
    UserPrincipal principal;
    @BeforeEach void setup() {
        hashtag = "review_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)", hashtag); user = id();
        principal = new UserPrincipal(user, hashtag);
        admin = jdbc.queryForObject("SELECT MIN(admin_no) FROM ultary_admin", Long.class);
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,profile_status,created_by_user_no) VALUES(?,'COMMERCIAL','PENDING',?)", hashtag, user); tag = id();
        jdbc.update("INSERT INTO ultary_tag_profile_request(tag_id,requester_user_no,title,status) VALUES(?,?,'review','PENDING')", tag, user); request = id();
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,'PUBLIC')", user); feed = id();
        jdbc.update("INSERT INTO ultary_notification_setting(user_no) VALUES(?)", user);
    }
    @Test void decisionsNotifyRequesterAndBothReviewedStatesCanReturnToPending() {
        assertThat(tags.search(hashtag, 20)).isEmpty();
        assertThat(tags.approveProfile(admin, tag).getStatus()).isEqualTo("APPROVED");
        assertThat(tags.search(hashtag, 20)).extracting(x -> x.getTagId()).containsExactly(tag);
        var approved = notifications.list(principal, 50).getItems();
        assertThat(approved).anySatisfy(row -> {
            assertThat(row.getType()).isEqualTo("TAG_PROFILE_APPROVED");
            assertThat(row.getMessage()).isEqualTo("#" + hashtag + " 의 태그 요청이 수락되었습니다.");
        });
        assertThat(tags.reopenProfile(tag).getStatus()).isEqualTo("PENDING");
        assertThat(tags.search(hashtag, 20)).isEmpty();
        tags.approveProfile(admin, tag);
        tags.rejectProfile(admin, tag, "다시 확인해 주세요");
        assertThat(tags.search(hashtag, 20)).isEmpty();
        var plain = tags.getDetail(tag, user);
        assertThat(plain.getPurpose()).isEqualTo("USER");
        assertThat(plain.getProfileStatus()).isEqualTo("REJECTED");
        assertThat(plain.getTitle()).isNull();
        assertThat(plain.getContent()).isNull();
        assertThat(plain.getImages()).isEmpty();
        assertThat(notifications.list(principal, 50).getItems()).anySatisfy(row -> {
            assertThat(row.getType()).isEqualTo("TAG_PROFILE_REJECTED");
            assertThat(row.getMessage()).isEqualTo("#" + hashtag + " 의 태그 요청이 거절되었습니다.");
        });
        assertThat(tags.reopenProfile(tag).getStatus()).isEqualTo("PENDING");
        assertThat(tags.search(hashtag, 20)).isEmpty();
    }
    @Test void rejectionPreservesExistingLinksButPreventsNewLinks() {
        tags.approveProfile(admin, tag);
        var update = new UpdateFeedRequest(); update.setTagIds(List.of(tag));
        feeds.update(principal, feed, update);
        tags.rejectProfile(admin, tag, "게시 중단");
        assertThat(feeds.getDetail(principal, feed).getTagIds()).containsExactly(tag);
        feeds.update(principal, feed, update);
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,'PUBLIC')", user); Long another = id();
        assertThatThrownBy(() -> feeds.update(principal, another, update)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_APPROVED));
        tags.reopenProfile(tag, request);
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility) VALUES(?,'PUBLIC')", user); Long pendingFeed = id();
        assertThatThrownBy(() -> feeds.update(principal, pendingFeed, update)).isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_APPROVED));
    }

    @Test void requestResponsesCountExistingFeedsAndRecordPendingTransitionTime() {
        var initial = tags.listMyProfileRequests(user, null, 30).get(0);
        assertThat(initial.getFeedCount()).isZero();
        assertThat(initial.getStatusChangedAt()).isNull();
        assertThat(tags.approveProfile(admin, tag, request).getStatusChangedAt()).isNotNull();
        var update = new UpdateFeedRequest(); update.setTagIds(List.of(tag));
        feeds.update(principal, feed, update);
        jdbc.update("INSERT INTO ultary_feed(user_no,visibility,is_deleted) VALUES(?,'PRIVATE',1)", user); Long removedFeed = id();
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)", removedFeed, tag);
        var pending = tags.reopenProfile(tag, request);
        assertThat(pending.getFeedCount()).isEqualTo(1);
        assertThat(pending.getStatusChangedAt()).isNotNull();
        assertThat(pending.getReviewedAt()).isNull();
        jdbc.update("UPDATE ultary_feed_tag SET is_deleted=1 WHERE feed_id=?", feed);
        assertThat(tags.listMyProfileRequests(user, null, 30).get(0).getFeedCount()).isZero();
    }

    @Test void restoringPreviouslyApprovedContentCanRestoreItsRejectedTagLink() {
        tags.approveProfile(admin, tag, request);
        var add = new UpdateFeedRequest(); add.setTagIds(List.of(tag)); feeds.update(principal, feed, add);
        tags.rejectProfile(admin, tag, "사용 중단", request);
        var remove = new UpdateFeedRequest(); remove.setTagIds(List.of()); feeds.update(principal, feed, remove);
        feeds.restoreCaptionTags(feed, "#" + hashtag);
        assertThat(feeds.getDetail(principal, feed).getTagIds()).containsExactly(tag);
        assertThat(tags.search(hashtag, 20)).isEmpty();
    }
    @Test void reviewTargetsTheSelectedRequestAndResubmissionReturnsTagToPending() {
        tags.approveProfile(admin, tag, request);
        var body = new me._hanho.ultary.domain.tag.dto.request.SubmitTagProfileRequest();
        body.setTitle("수정 요청");
        Long newer = tags.submitProfile(principal, tag, body).getRequestId();
        assertThat(newer).isNotEqualTo(request);
        assertThat(tags.search(hashtag, 20)).isEmpty();
        assertThat(tags.reopenProfile(tag, request).getRequestId()).isEqualTo(request);
        assertThat(tags.approveProfile(admin, tag, request).getRequestId()).isEqualTo(request);
        assertThat(jdbc.queryForObject("SELECT status FROM ultary_tag_profile_request WHERE tag_profile_request_id=?", String.class, newer)).isEqualTo("PENDING");
        assertThat(tags.rejectProfile(admin, tag, "재검토", request).getRequestId()).isEqualTo(request);
    }
    Long id() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }

    @Test void recentDecisionNoticesAreReplacedAndReturningToPendingRemovesThem() {
        tags.approveProfile(admin, tag, request);
        assertThat(noticeCount()).isEqualTo(1);
        tags.rejectProfile(admin, tag, "재검토", request);
        assertThat(noticeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT type FROM ultary_notification WHERE receiver_user_no=?", String.class, user)).isEqualTo("TAG_PROFILE_REJECTED");
        tags.reopenProfile(tag, request);
        assertThat(noticeCount()).isZero();
        assertThat(notifications.unreadCount(principal).getUnreadCount()).isZero();
    }

    @Test void olderNoticesAndAnotherRequestRemainWhileNewNoticesUseTheirOwnCreationTime() {
        tags.approveProfile(admin, tag, request);
        jdbc.update("UPDATE ultary_notification SET created_at=DATE_SUB(NOW(), INTERVAL 3 MINUTE) WHERE receiver_user_no=?", user);
        jdbc.update("INSERT INTO ultary_tag_profile_request(tag_id,requester_user_no,title,status) VALUES(?,?,'another','PENDING')", tag, user);
        Long otherRequest = id();
        notifications.notifyTagReview(user, otherRequest, hashtag, true);
        tags.rejectProfile(admin, tag, "재검토", request);
        assertThat(noticeCount()).isEqualTo(3);
        tags.reopenProfile(tag, request);
        assertThat(noticeCount()).isEqualTo(2);
        tags.approveProfile(admin, tag, request);
        assertThat(noticeCount()).isEqualTo(3);
        tags.reopenProfile(tag, request);
        assertThat(noticeCount()).isEqualTo(2);
    }

    @Test void legacyNoticeKeysAreAlsoCleanedAndSameStatusDoesNotRemoveAnExistingNotice() {
        tags.approveProfile(admin, tag, request);
        tags.approveProfile(admin, tag, request);
        assertThat(noticeCount()).isEqualTo(1);
        jdbc.update("UPDATE ultary_notification SET group_key=? WHERE receiver_user_no=?", "TAG_PROFILE_APPROVED:" + request, user);
        tags.reopenProfile(tag, request);
        assertThat(noticeCount()).isZero();
    }

    int noticeCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?", Integer.class, user); }

    @Test void rejectedRequestReturnsToPendingThroughTheAdminFormEndpoint() throws Exception {
        tags.rejectProfile(admin, tag, "잘못된 URL", request);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/tags/" + tag + "/pending")
            .secure(true).header("X-BFF-Key", bff.getSharedKey())
            .header("Authorization", "Bearer " + jwt.createAdminAccessToken(admin))
            .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
            .param("requestId", request.toString()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.requestId").value(request))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.status").value("PENDING"));
        assertThat(jdbc.queryForObject("SELECT status FROM ultary_tag_profile_request WHERE tag_profile_request_id=?", String.class, request)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT profile_status FROM ultary_tag WHERE tag_id=?", String.class, tag)).isEqualTo("PENDING");
        assertThat(noticeCount()).isZero();
    }
}
