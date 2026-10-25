package me._hanho.ultary.domain.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import me._hanho.ultary.domain.admin.dto.response.AdminAiLogResponse;

import me._hanho.ultary.domain.ai.model.AiRequestLog;

@Mapper
public interface AiRequestLogMapper {

	int insert(AiRequestLog log);

    int countFailed();

    List<AdminAiLogResponse> listForAdmin(@Param("status") String status,
            @Param("featureType") String featureType, @Param("targetType") String targetType, @Param("targetId") Long targetId,
            @Param("errorCode") String errorCode, @Param("userNo") Long userNo,
            @Param("moderationJobId") Long moderationJobId, @Param("cursorAiRequestId") Long cursorAiRequestId,
            @Param("limit") int limit, @Param("phone") String phone, @Param("email") String email,
            @Param("cursorPriority") Integer cursorPriority);

    AdminAiLogResponse findForAdmin(@Param("aiRequestId") Long aiRequestId);

    List<AdminAiLogResponse> attemptsForAdmin(@Param("moderationJobId") Long moderationJobId, @Param("aiRequestId") Long aiRequestId);

    int countForAdmin(@Param("status") String status, @Param("featureType") String featureType,
            @Param("targetType") String targetType, @Param("targetId") Long targetId,
            @Param("errorCode") String errorCode, @Param("userNo") Long userNo,
            @Param("moderationJobId") Long moderationJobId, @Param("phone") String phone, @Param("email") String email);
}
