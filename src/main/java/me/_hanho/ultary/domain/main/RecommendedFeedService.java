package me._hanho.ultary.domain.main;

import java.util.*;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.feed.FeedMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.feed.FeedAdvertisementPolicy;
import me._hanho.ultary.domain.feed.model.Feed;
import me._hanho.ultary.domain.main.dto.response.RecommendedFeedPageResponse;

@Service
@RequiredArgsConstructor
public class RecommendedFeedService {
    private final FeedMapper mapper;
    private final FeedService feeds;
    private final ObjectMapper json;
    public record Cursor(long viewer, boolean search, List<Long> remaining) {}

    @Transactional(readOnly=true)
    public RecommendedFeedPageResponse list(Long viewer, boolean search, Integer requested, String cursor) {
        int size=requested==null?(search?18:10):requested;
        if(size<1||size>50)throw invalid();
        List<RecommendedFeedCandidate> current=mapper.findRecommendedCandidates(viewer,search);
        if (!search) current=current.stream().filter(row -> !FeedAdvertisementPolicy.isAdvertisement(row.getUserNo())).toList();
        // Small portfolio dataset: snapshot the ordered candidate IDs, then recheck visibility on every page.
        List<RecommendedFeedCandidate> candidates=current;
        if(cursor!=null&&!cursor.isBlank()) {
            Cursor saved=decode(cursor);
            if(saved.viewer()!=viewer||saved.search()!=search)throw invalid();
            Map<Long,RecommendedFeedCandidate> visible=new HashMap<>();
            for(var row:current)visible.put(row.getFeedId(),row);
            candidates=saved.remaining().stream().map(visible::get).filter(Objects::nonNull).toList();
        }
        List<RecommendedFeedCandidate> picked=selectPage(candidates,size);
        Set<Long> selected=new HashSet<>();for(var row:picked)selected.add(row.getFeedId());
        List<Long> remaining=candidates.stream().map(Feed::getFeedId).filter(id->!selected.contains(id)).toList();
        String next=remaining.isEmpty()?null:encode(new Cursor(viewer,search,remaining));
        return new RecommendedFeedPageResponse(feeds.toResponses(new ArrayList<Feed>(picked),viewer),next);
    }

    static List<RecommendedFeedCandidate> selectPage(List<RecommendedFeedCandidate> rows,int size) {
        List<RecommendedFeedCandidate> page=new ArrayList<>();
        Map<Long,Integer> authorCounts=new HashMap<>();Set<Long> chosen=new HashSet<>();
        for(var row:rows) {
            if(page.size()==size)break;
            if(authorCounts.getOrDefault(row.getUserNo(),0)>=2)continue;
            page.add(row);chosen.add(row.getFeedId());authorCounts.merge(row.getUserNo(),1,Integer::sum);
        }
        // Insufficient variety must not permanently hide otherwise eligible posts.
        for(var row:rows) {
            if(page.size()==size)break;
            if(chosen.add(row.getFeedId()))page.add(row);
        }
        return page;
    }

    @Transactional(readOnly=true)
    public List<me._hanho.ultary.domain.feed.dto.response.FeedResponse> advertisements(Long viewer) {
        // search=true includes followed sellers too. Existing visibility/block/media filters still apply.
        List<Feed> candidates=new ArrayList<>(mapper.findRecommendedCandidates(viewer,true).stream()
            .filter(row -> FeedAdvertisementPolicy.isAdvertisement(row.getUserNo()) && row.isCommercial())
            .toList());
        Collections.shuffle(candidates);
        return feeds.toResponses(candidates.stream().limit(50).toList(),viewer);
    }

    private Cursor decode(String encoded) {
        try {
            if(encoded.length()>65536)throw invalid();
            Cursor value=json.readValue(Base64.getUrlDecoder().decode(encoded),Cursor.class);
            if(value.remaining()==null||value.remaining().stream().anyMatch(id->id==null||id<=0)
                    ||new HashSet<>(value.remaining()).size()!=value.remaining().size())throw invalid();
            return value;
        }catch(Exception error){throw invalid();}
    }
    private String encode(Cursor cursor) {
        try{return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(cursor));}
        catch(Exception error){throw new IllegalStateException("Cannot encode recommendation cursor",error);}
    }
    private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_INPUT,"추천 목록 커서 또는 조회 개수가 올바르지 않습니다.");}
}
