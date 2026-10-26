package me._hanho.ultary.domain.seo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface SeoMapper {
 List<SeoEntry> profiles(@Param("cursorId") Long cursorId, @Param("limit") int limit);
 List<SeoEntry> feeds(@Param("cursorId") Long cursorId, @Param("limit") int limit);
}
