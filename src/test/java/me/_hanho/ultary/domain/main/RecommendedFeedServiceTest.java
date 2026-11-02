package me._hanho.ultary.domain.main;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.domain.feed.model.Feed;

class RecommendedFeedServiceTest {
    RecommendedFeedCandidate row(long id,long author,boolean commercial) {
        var row=new RecommendedFeedCandidate();row.setFeedId(id);row.setUserNo(author);row.setCommercial(commercial);return row;
    }
    @Test void fullPagePrioritizesDifferentAuthorsWithoutTheOldCommercialQuota() {
        List<RecommendedFeedCandidate> rows=new ArrayList<>();
        for(int i=0;i<8;i++)rows.add(row(i+1,1,true));
        for(int i=0;i<8;i++)rows.add(row(i+20,i+2,i<3));
        for(int i=0;i<12;i++)rows.add(row(i+50,i+10,false));
        var result=RecommendedFeedService.selectPage(rows,10);
        assertThat(result).hasSize(10);
        assertThat(result.stream().filter(RecommendedFeedCandidate::isCommercial).count()).isEqualTo(5);
        for(var author:result.stream().map(Feed::getUserNo).distinct().toList())
            assertThat(result.stream().filter(r->r.getUserNo().equals(author)).count()).isLessThanOrEqualTo(2);
    }
    @Test void limitedVarietyRelaxesQuotasWithoutLosingOrDuplicatingPosts() {
        var rows=List.of(row(1,1,true),row(2,1,true),row(3,1,true));
        assertThat(RecommendedFeedService.selectPage(rows,10)).extracting(Feed::getFeedId).containsExactly(1L,2L,3L);
    }
    @Test void cursorKeepsRemainingOrderRechecksVisibilityAndDoesNotAppendNewPosts() {
        FeedMapper mapper=mock(FeedMapper.class);FeedService feeds=mock(FeedService.class);
        when(feeds.toResponses(anyList(),anyLong())).thenAnswer(invocation->((List<Feed>)invocation.getArgument(0)).stream()
                .map(r->FeedResponse.builder().feedId(r.getFeedId()).build()).toList());
        var service=new RecommendedFeedService(mapper,feeds,new ObjectMapper());
        when(mapper.findRecommendedCandidates(1L,true)).thenReturn(List.of(row(1,2,false),row(2,3,false),row(3,4,false),row(4,5,false)));
        var first=service.list(1L,true,2,null);
        assertThat(first.items()).extracting(FeedResponse::getFeedId).containsExactly(1L,2L);
        when(mapper.findRecommendedCandidates(1L,true)).thenReturn(List.of(row(99,9,false),row(4,5,false)));
        var next=service.list(1L,true,2,first.nextCursor());
        assertThat(next.items()).extracting(FeedResponse::getFeedId).containsExactly(4L);
        assertThat(next.nextCursor()).isNull();
        assertThatThrownBy(()->service.list(2L,true,2,first.nextCursor())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.list(1L,false,2,first.nextCursor())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.list(1L,true,2,"bad cursor")).isInstanceOf(BusinessException.class);
    }
    @Test void mainKeepsSellerPostsOutOfNormalPagesAndAdsIncludeFollowedSellersOnlyWhenCommercial() {
        FeedMapper mapper=mock(FeedMapper.class);FeedService feeds=mock(FeedService.class);
        when(feeds.toResponses(anyList(),anyLong())).thenAnswer(invocation->((List<Feed>)invocation.getArgument(0)).stream()
            .map(r->FeedResponse.builder().feedId(r.getFeedId()).build()).toList());
        var service=new RecommendedFeedService(mapper,feeds,new ObjectMapper());
        var seller=row(11,6129,true);seller.setFollowing(true);
        var unapproved=row(12,6130,false);var review=row(13,2,true);
        when(mapper.findRecommendedCandidates(1L,false)).thenReturn(List.of(unapproved,review));
        when(mapper.findRecommendedCandidates(1L,true)).thenReturn(List.of(seller,unapproved,review));
        assertThat(service.list(1L,false,10,null).items()).extracting(FeedResponse::getFeedId).containsExactly(13L);
        assertThat(service.advertisements(1L)).extracting(FeedResponse::getFeedId).containsExactly(11L);
    }

    @Test void emptyCandidatesFinishAndSearchDefaultsToEighteen() {
        FeedMapper mapper=mock(FeedMapper.class);FeedService feeds=mock(FeedService.class);
        when(mapper.findRecommendedCandidates(1L,true)).thenReturn(List.of());
        when(feeds.toResponses(anyList(),eq(1L))).thenReturn(List.of());
        var service=new RecommendedFeedService(mapper,feeds,new ObjectMapper());
        assertThat(service.list(1L,true,null,null).nextCursor()).isNull();
        assertThatThrownBy(()->service.list(1L,true,0,null)).isInstanceOf(BusinessException.class);
    }
}
