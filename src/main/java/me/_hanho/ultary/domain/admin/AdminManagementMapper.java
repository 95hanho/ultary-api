package me._hanho.ultary.domain.admin;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import me._hanho.ultary.domain.admin.dto.response.*;

@Mapper
public interface AdminManagementMapper {
    List<AdminUserResponse> users(@Param("q") String q, @Param("status") String status,
            @Param("offset") int offset, @Param("limit") int limit);
    int countUsers(@Param("q") String q, @Param("status") String status);
    AdminUserResponse user(@Param("userNo") Long userNo);
    List<AdminUserDetailResponse.FeedItem> feeds(@Param("userNo") Long userNo);
    List<AdminUserDetailResponse.ReportItem> reports(@Param("userNo") Long userNo);
    List<AdminActionLogResponse> actions(@Param("q") String q, @Param("targetType") String targetType,
            @Param("offset") int offset, @Param("limit") int limit);
    int countActions(@Param("q") String q, @Param("targetType") String targetType);
    AdminActionLogResponse target(@Param("kind") String kind, @Param("id") Long id, @Param("requestId") Long requestId);
    int insertAction(AdminActionLogResponse entry);

    List<AdminTimelineRow> deletedStories(@Param("userNo") Long userNo, @Param("limit") int limit);
    List<AdminTimelineRow> deletedFeeds(@Param("userNo") Long userNo, @Param("limit") int limit);
    List<AdminTimelineRow> deletedPets(@Param("userNo") Long userNo, @Param("limit") int limit);
    AdminTimelineRow passwordChanged(@Param("userNo") Long userNo);
    AdminTimelineRow nicknameChanged(@Param("userNo") Long userNo);
    AdminTimelineRow accountWithdrawn(@Param("userNo") Long userNo);

    List<String> contentPetNames(@Param("type") String type, @Param("id") Long id);
    List<Long> feedMediaFileIds(@Param("feedId") Long feedId);

    int countOwnedTimelineFile(@Param("userNo") Long userNo, @Param("fileId") Long fileId);
    Long deletedFeedOwner(@Param("feedId") Long feedId);
}
