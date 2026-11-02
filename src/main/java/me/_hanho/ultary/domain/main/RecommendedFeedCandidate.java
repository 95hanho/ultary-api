package me._hanho.ultary.domain.main;

import lombok.Getter;
import lombok.Setter;
import me._hanho.ultary.domain.feed.model.Feed;

@Getter
@Setter
public class RecommendedFeedCandidate extends Feed {
    private boolean following;
    private boolean commercial;
}
