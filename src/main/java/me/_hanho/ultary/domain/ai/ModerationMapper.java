package me._hanho.ultary.domain.ai;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import me._hanho.ultary.domain.ai.model.*;

@Mapper
public interface ModerationMapper {
    ModerationTarget lockTarget(@Param("kind") String kind, @Param("id") Long id);
    int ensureState(@Param("kind") String kind, @Param("id") Long id,
                    @Param("previous") String previous, @Param("trusted") boolean trusted);
    ModerationState lockState(@Param("kind") String kind, @Param("id") Long id);
    int saveState(ModerationState state);
    int insertJob(ModerationJob job);
    List<Long> findReady(@Param("limit") int limit);
    int claim(@Param("id") Long id, @Param("token") String token);
    ModerationJob findJob(@Param("id") Long id);
    ModerationJob lockJob(@Param("id") Long id);
    int finishJob(ModerationJob job);
    int restoreContent(@Param("kind") String kind, @Param("id") Long id, @Param("content") String content);
    int hide(@Param("kind") String kind, @Param("id") Long id);
}
