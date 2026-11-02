package me._hanho.ultary.domain.main;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import me._hanho.ultary.domain.feed.FeedMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="ai.moderation.worker-enabled=false")
@Transactional
class RecommendedFeedIntegrationTest {
    @Autowired FeedMapper mapper;
    @Autowired RecommendedFeedService service;
    @Autowired me._hanho.ultary.domain.feed.FeedService feeds;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;
    Long viewer,author,followed,own,publicFeed,followedFeed,privateFeed,neighborsFeed,blockedFeed,privateAccountFeed,deletedFeed,noMediaFeed;
    @BeforeEach void fixture() {
        viewer=user();author=user();followed=user();
        own=feed(viewer,"PUBLIC",true);publicFeed=feed(author,"PUBLIC",true);
        followedFeed=feed(followed,"PUBLIC",true);
        privateFeed=feed(author,"PRIVATE",true);neighborsFeed=feed(author,"NEIGHBORS",true);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status,accepted_at) VALUES(?,?,?,'ACCEPTED',NOW())",viewer,followed,Math.min(viewer,followed)+":"+Math.max(viewer,followed));
        Long blocked=user();blockedFeed=feed(blocked,"PUBLIC",true);
        jdbc.update("INSERT INTO ultary_user_block(blocker_user_no,blocked_user_no) VALUES(?,?)",blocked,viewer);
        Long privateOwner=user();privateAccountFeed=feed(privateOwner,"PUBLIC",true);
        jdbc.update("INSERT INTO ultary_user_privacy(user_no,private_account) VALUES(?,1)",privateOwner);
        deletedFeed=feed(author,"PUBLIC",true);jdbc.update("UPDATE ultary_feed SET is_deleted=1 WHERE feed_id=?",deletedFeed);
        noMediaFeed=feed(author,"PUBLIC",false);
    }
    @Test void mainExcludesOwnFollowedPrivateBlockedAndUnrenderablePostsWhileSearchIncludesFollowedLast() {
        var main=mapper.findRecommendedCandidates(viewer,false);
        assertThat(main).extracting(r->r.getFeedId()).contains(publicFeed)
                .doesNotContain(own,followedFeed,privateFeed,neighborsFeed,blockedFeed,privateAccountFeed,deletedFeed,noMediaFeed);
        var search=mapper.findRecommendedCandidates(viewer,true);
        assertThat(search).extracting(r->r.getFeedId()).contains(publicFeed,followedFeed)
                .doesNotContain(own,privateFeed,neighborsFeed,blockedFeed,privateAccountFeed,deletedFeed,noMediaFeed);
        assertThat(search.get(search.size()-1).getFeedId()).isEqualTo(followedFeed);
    }
    @Test void paginationExhaustsAllEligiblePostsWithoutOverlapAndDropsPostsMadePrivateBetweenPages() {
        var candidates=mapper.findRecommendedCandidates(viewer,true);
        var first=service.list(viewer,true,1,null);
        Set<Long> seen=new HashSet<>();first.items().forEach(row->seen.add(row.getFeedId()));
        // Followed posts are ordered last, so this must still be pending after page one.
        assertThat(seen).doesNotContain(followedFeed);
        jdbc.update("UPDATE ultary_feed SET visibility='PRIVATE' WHERE feed_id=?",followedFeed);
        sqlSession.clearCache(); // Simulate the next HTTP request's fresh MyBatis session.
        String cursor=first.nextCursor();int pages=0;
        while(cursor!=null) {
            assertThat(++pages).isLessThan(200);
            var page=service.list(viewer,true,18,cursor);
            for(var row:page.items())assertThat(seen.add(row.getFeedId())).isTrue();
            cursor=page.nextCursor();
        }
        assertThat(seen).doesNotContain(followedFeed).contains(publicFeed);
        assertThat(seen).hasSize(candidates.size()-1);
    }
    @Test void followerOnlyIsNotMistakenForFollowingAndFollowBackExcludesMainRecommendation() {
        Long follower=user(),post=feed(follower,"PUBLIC",true);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status) VALUES(?,?,?,'ACCEPTED')",follower,viewer,Math.min(viewer,follower)+":"+Math.max(viewer,follower));
        assertThat(mapper.findRecommendedCandidates(viewer,false)).extracting(r->r.getFeedId()).contains(post);
        jdbc.update("UPDATE ultary_neighbor SET followed_back=1 WHERE requester_user_no=? AND receiver_user_no=?",follower,viewer);
        sqlSession.clearCache();
        assertThat(mapper.findRecommendedCandidates(viewer,false)).extracting(r->r.getFeedId()).doesNotContain(post);
        assertThat(mapper.findRecommendedCandidates(viewer,true)).extracting(r->r.getFeedId()).contains(post);
    }
    @Test void onlyApprovedActiveCommercialTagsMarkProductPosts() {
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,profile_status,created_by_user_no) VALUES(?,'COMMERCIAL','APPROVED',?)","상품"+UUID.randomUUID().toString().substring(0,8),author);
        Long tag=id();jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)",publicFeed,tag);
        assertThat(mapper.findRecommendedCandidates(viewer,true).stream().filter(r->r.getFeedId().equals(publicFeed)).findFirst().orElseThrow().isCommercial()).isTrue();
        jdbc.update("UPDATE ultary_tag SET profile_status='REJECTED' WHERE tag_id=?",tag);
        sqlSession.clearCache();
        assertThat(mapper.findRecommendedCandidates(viewer,true).stream().filter(r->r.getFeedId().equals(publicFeed)).findFirst().orElseThrow().isCommercial()).isFalse();
    }
    @Test void followedSellerIsExcludedFromTimelineButEligibleForAdInsertionAndAlwaysLabelled() {
        Long seller=6129L;
        Long ad=feed(seller,"PUBLIC",true), hidden=feed(seller,"PRIVATE",true);
        jdbc.update("INSERT INTO ultary_tag(hashtag,purpose,profile_status,created_by_user_no) VALUES(?,'COMMERCIAL','APPROVED',?)","ad_"+UUID.randomUUID().toString().substring(0,8),seller);
        Long product=id();
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)",ad,product);
        jdbc.update("INSERT INTO ultary_feed_tag(feed_id,tag_id) VALUES(?,?)",hidden,product);
        jdbc.update("INSERT INTO ultary_neighbor(requester_user_no,receiver_user_no,pair_key,status) VALUES(?,?,?,'ACCEPTED')",viewer,seller,Math.min(viewer,seller)+":"+Math.max(viewer,seller));
        assertThat(mapper.findMainTimeline(viewer,null,null,50,me._hanho.ultary.domain.feed.FeedAdvertisementPolicy.SELLER_USER_NOS))
            .extracting(r->r.getFeedId()).doesNotContain(ad,hidden).contains(own,followedFeed);
        assertThat(service.advertisements(viewer)).extracting(r->r.getFeedId()).contains(ad).doesNotContain(hidden,publicFeed);
        assertThat(feeds.getDetail(new me._hanho.ultary.security.principal.UserPrincipal(viewer,"test"),ad).isAdvertisement()).isTrue();
        assertThat(feeds.getDetail(new me._hanho.ultary.security.principal.UserPrincipal(viewer,"test"),publicFeed).isAdvertisement()).isFalse();
    }

    Long user(){jdbc.update("INSERT INTO ultary_user(nickname) VALUES(?)","rec_"+UUID.randomUUID().toString().substring(0,9));return id();}
    Long id(){return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    Long feed(Long owner,String visibility,boolean media){
        jdbc.update("INSERT INTO ultary_feed(user_no,content,visibility) VALUES(?,'추천 검증',?)",owner,visibility);Long feed=id();
        if(media){jdbc.update("INSERT INTO ultary_file(store_name,file_path,mime_type,extension,source_type,uploaded_by_user_no) VALUES('rec.jpg','rec.jpg','image/jpeg','jpg','OWNED',?)",owner);Long file=id();jdbc.update("INSERT INTO ultary_feed_media(feed_id,file_id,media_type,sort_order) VALUES(?,?,'IMAGE',0)",feed,file);}
        return feed;
    }
}
