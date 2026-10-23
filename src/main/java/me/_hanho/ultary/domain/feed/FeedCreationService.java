package me._hanho.ultary.domain.feed;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.request.UpdateFeedRequest;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** Inspect before opening the content write transaction. */
@Service
@RequiredArgsConstructor
public class FeedCreationService {
    private final AiService ai;
    private final FeedService feeds;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FeedResponse create(UserPrincipal principal, CreateFeedRequest request) {
        if (request.getMedia() == null || request.getMedia().isEmpty()) {
            throw new BusinessException(ErrorCode.FEED_MEDIA_REQUIRED);
        }
        ai.assertFeedCreationAllowed(principal.getUserNo(), request.getContent());
        return feeds.create(principal, request);
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FeedResponse update(UserPrincipal principal, Long feedId, UpdateFeedRequest request) {
        FeedResponse current = feeds.getDetail(principal, feedId);
        if (!principal.getUserNo().equals(current.getUserNo())) throw new BusinessException(ErrorCode.FORBIDDEN);
        String content = Boolean.TRUE.equals(request.getClearContent()) ? null
                : request.getContent() != null ? request.getContent() : current.getContent();
        ai.assertFeedUpdateAllowed(principal.getUserNo(), feedId, content);
        return feeds.update(principal, feedId, request);
    }
}
