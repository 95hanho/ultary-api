package me._hanho.ultary.domain.tag;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.tag.model.Tag;

@Mapper
public interface TagMapper {

	Tag findActiveByTagId(@Param("tagId") Long tagId);

	int countByHandle(
			@Param("handle") String handle,
			@Param("excludeTagId") Long excludeTagId);

	int updateHandle(
			@Param("tagId") Long tagId,
			@Param("createdByUserNo") Long createdByUserNo,
			@Param("handle") String handle);
}
