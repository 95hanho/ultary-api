package me._hanho.ultary.domain.feed;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.feed.dto.request.*;
import me._hanho.ultary.domain.ai.*;
import me._hanho.ultary.domain.notification.NotificationService;
import me._hanho.ultary.security.principal.UserPrincipal;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="ai.moderation.worker-enabled=false")
@Transactional
class CommentMentionIntegrationTest {
 @Autowired FeedService feeds; @Autowired FeedMapper mapper; @Autowired JdbcTemplate jdbc;
 @Autowired me._hanho.ultary.domain.dm.DmService dm;
 @Autowired NotificationService notices; @Autowired ModerationMapper moderation; @Autowired ModerationTransactions transactions;
 Long actor,owner,parentAuthor,target,feed,parent; UserPrincipal principal;
 @BeforeEach void fixture(){actor=user();owner=user();parentAuthor=user();target=user();principal=new UserPrincipal(actor,"test");
  jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'mention fixture','PUBLIC')",owner);feed=id();
  jdbc.update("INSERT INTO ultary_feed_comment(feed_id,user_no,content) VALUES(?,?,'parent')",feed,parentAuthor);parent=id();}
 @Test void typedAndExplicitMentionsPersistOneIdentityAndNotifyWithCommentTarget(){
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" hi");req.setMentions(List.of(mention(target)));req.setMentions(List.of(mention(target),mention(target)));
  var result=feeds.createComment(principal,feed,req);assertThat(result.getMentions()).hasSize(1);
  assertThat(mapper.findMentionsByCommentId(result.getFeedCommentId())).extracting(x->x.getMentionedUserNo()).containsExactly(target);
  var notice=notices.list(new UserPrincipal(target,"test"),50).getItems();
  assertThat(notice).hasSize(1);assertThat(notice.get(0).getMessage()).isEqualTo(name(actor)+"가 댓글에서 나를 언급하였습니다.");
  assertThat(notice.get(0).getFeedCommentId()).isEqualTo(result.getFeedCommentId());
 }
 @Test void parentReceivesOnlyRegularReplyWhileOtherMentionReceivesReplyMention(){
  var req=new CreateReplyRequest();req.setContent("@"+name(parentAuthor)+" @"+name(target)+" hello");req.setMentions(List.of(mention(parentAuthor),mention(target)));
  var reply=feeds.createReply(principal,feed,parent,req);
  assertThat(mapper.findMentionsByReplyId(reply.getFeedReplyId())).hasSize(2);
  assertThat(dm.shareTargets(principal,50,null,null,"COMMENT_MENTION").getItems()).extracting(x->x.getUserNo()).contains(target,parentAuthor);
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND type='REPLY_MENTION'",parentAuthor)).isZero();
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND type='FEED_COMMENT'",parentAuthor)).isEqualTo(1);
  var notice=notices.list(new UserPrincipal(target,"test"),50).getItems().get(0);
  assertThat(notice.getMessage()).isEqualTo(name(actor)+"가 답글에서 나를 언급하였습니다.");
  assertThat(notice.getFeedReplyId()).isEqualTo(reply.getFeedReplyId());assertThat(notice.getFeedCommentId()).isEqualTo(parent);
 }
 @Test void editingPreservesUnchangedLinkAgeAndReadStateThenRemovesDeletedMention(){
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" hi");req.setMentions(List.of(mention(target)));var comment=feeds.createComment(principal,feed,req);
  var link=mapper.findMentionsByCommentId(comment.getFeedCommentId()).get(0);
  jdbc.update("UPDATE ultary_notification SET is_read=1 WHERE receiver_user_no=?",target);
  var edit=new UpdateCommentRequest();edit.setContent("@"+name(target)+" changed");edit.setMentions(List.of(mention(target)));feeds.updateComment(principal,feed,comment.getFeedCommentId(),edit);
  assertThat(mapper.findMentionsByCommentId(comment.getFeedCommentId()).get(0).getFeedCommentMentionId()).isEqualTo(link.getFeedCommentMentionId());
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=? AND is_read=0",target)).isZero();
  edit.setContent("no mention");feeds.updateComment(principal,feed,comment.getFeedCommentId(),edit);
  assertThat(mapper.findMentionsByCommentId(comment.getFeedCommentId())).isEmpty();
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?",target)).isZero();
 }
 @Test void privacyRejectionRollsBackTheCommentAndMention(){
  jdbc.update("INSERT INTO ultary_user_privacy(user_no,allow_mention) VALUES(?,0)",target);
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" hi");req.setMentions(List.of(mention(target)));
  assertThatThrownBy(()->feeds.createComment(principal,feed,req)).isInstanceOf(me._hanho.ultary.common.exception.BusinessException.class);
 }
 @Test void rejectedEditRestoresMentionIdentityWithApprovedText(){
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" hi");req.setMentions(List.of(mention(target)));var comment=feeds.createComment(principal,feed,req);
  Long first=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE target_type='COMMENT' AND target_id=?",Long.class,comment.getFeedCommentId());
  transactions.complete(transactions.claim(first),ModerationResult.allowed());
  var edit=new UpdateCommentRequest();edit.setContent("bad edit without mention");feeds.updateComment(principal,feed,comment.getFeedCommentId(),edit);
  Long last=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE target_type='COMMENT' AND target_id=?",Long.class,comment.getFeedCommentId());
  jdbc.update("INSERT INTO ultary_user_privacy(user_no,allow_mention) VALUES(?,0)",target);
  transactions.complete(transactions.claim(last),new ModerationResult(false,"ABUSE",null,null,false));
  assertThat(mapper.findMentionsByCommentId(comment.getFeedCommentId())).extracting(x->x.getMentionedUserNo()).containsExactly(target);
 }

 @Test void typedNamesWithoutSelectionStayPlainAndCannotSendMentionNotifications(){
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" plain");var result=feeds.createComment(principal,feed,req);
  assertThat(mapper.findMentionsByCommentId(result.getFeedCommentId())).isEmpty();
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?",target)).isZero();
  req.setContent("attached@"+name(target));req.setMentions(List.of(mention(target)));
  assertThat(feeds.createComment(principal,feed,req).getMentions()).isEmpty();
 }
 @Test void rejectionRestoresPlainTextWithoutInferringASelectedMention(){
  var req=new CreateCommentRequest();req.setContent("@"+name(target)+" original plain");var comment=feeds.createComment(principal,feed,req);
  Long first=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE target_type='COMMENT' AND target_id=?",Long.class,comment.getFeedCommentId());
  transactions.complete(transactions.claim(first),ModerationResult.allowed());
  var edit=new UpdateCommentRequest();edit.setContent("@"+name(target)+" bad selected");edit.setMentions(List.of(mention(target)));feeds.updateComment(principal,feed,comment.getFeedCommentId(),edit);
  Long last=jdbc.queryForObject("SELECT MAX(job_id) FROM ultary_ai_moderation_job WHERE target_type='COMMENT' AND target_id=?",Long.class,comment.getFeedCommentId());
  transactions.complete(transactions.claim(last),new ModerationResult(false,"ABUSE",null,null,false));
  assertThat(mapper.findMentionsByCommentId(comment.getFeedCommentId())).isEmpty();
  assertThat(count("SELECT COUNT(*) FROM ultary_notification WHERE receiver_user_no=?",target)).isZero();
 }
 Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);} Long user(){jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","mention"+UUID.randomUUID().toString().replace("-","").substring(0,10));return id();}
 String name(Long user){return jdbc.queryForObject("SELECT nickname FROM ultary_user WHERE user_no=?",String.class,user);}
 int count(String sql,Long user){return jdbc.queryForObject(sql,Integer.class,user);}
 CommentMentionRequest mention(Long user){var item=new CommentMentionRequest();item.setUserNo(user);return item;}
}
