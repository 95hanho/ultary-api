package me._hanho.ultary.domain.myultary.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

/** Anonymous, public account projection without private account or relationship data. */
public record PublicUltaryResponse(Long userNo, String nickname, String bio,
        int followCount, int followerCount, int feedCount, List<PublicPet> pets,
        List<FeedGridItemResponse> feeds, List<FeedGridItemResponse> pinnedFeeds,
        List<FeedGridItemResponse> taggedFeeds) {
    public record PublicPet(Long petId, String name, String mentionId, String gender,
            String bio, LocalDateTime birthday, FileSummaryResponse profileFile) {}
}
