package me._hanho.ultary.domain.tag;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.tag.model.TagImage;
import me._hanho.ultary.domain.tag.model.TagSearchHit;

@Mapper
public interface TagMapper {

	Tag findActiveByTagId(@Param("tagId") Long tagId);

	int insert(Tag tag);

	int update(Tag tag);

	List<Tag> search(
			@Param("q") String q,
			@Param("limit") int limit);

	/** 해시태그 포함 검색. feedCount는 조회자에게 보이는 게시글 수 */
	List<TagSearchHit> searchByHashtag(
			@Param("viewerUserNo") Long viewerUserNo,
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

	/** 삭제되지 않은 게시글 중 이 태그가 달린 수 */
	int countActiveFeeds(@Param("tagId") Long tagId);
}
