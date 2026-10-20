package me._hanho.ultary.domain.feed;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiService;
import me._hanho.ultary.domain.feed.dto.request.CreateFeedRequest;
import me._hanho.ultary.domain.feed.dto.response.FeedResponse;
import me._hanho.ultary.security.principal.UserPrincipal;

/** Wait for moderation before opening the atomic feed-write transaction. */
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
}
