package me._hanho.ultary.domain.tag;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.tag.model.TagImage;

@Mapper
public interface TagMapper {

	Tag findActiveByTagId(@Param("tagId") Long tagId);

	int insert(Tag tag);

	int update(Tag tag);

	List<Tag> search(
			@Param("q") String q,
			@Param("limit") int limit);

	List<Tag> recommend(
			@Param("q") String q,
			@Param("limit") int limit);

	int countByHandle(
			@Param("handle") String handle,
			@Param("excludeTagId") Long excludeTagId);

	int updateHandle(
			@Param("tagId") Long tagId,
			@Param("createdByUserNo") Long createdByUserNo,
			@Param("handle") String handle);

	int insertImage(TagImage image);

	List<Long> findImageFileIdsByTagId(@Param("tagId") Long tagId);

	int incrementUseCount(@Param("tagId") Long tagId);
}
