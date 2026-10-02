package me._hanho.ultary.domain.activity;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.activity.model.ActivityRow;

@Mapper
public interface ActivityMapper {

	List<ActivityRow> findByUser(@Param("userNo") Long userNo, @Param("limit") int limit);
}
