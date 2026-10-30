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
}
