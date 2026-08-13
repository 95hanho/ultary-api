package me._hanho.ultary.domain.story;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.story.model.Story;
import me._hanho.ultary.domain.story.model.StoryOwnerRow;

@Mapper
public interface StoryMapper {

	int insert(Story story);

	Story findActiveByStoryId(@Param("storyId") Long storyId);

	List<Story> findActiveByUserNo(@Param("userNo") Long userNo);

	int countActiveByUserNo(@Param("userNo") Long userNo);

	int softDelete(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);

	int insertViewIgnoreDuplicate(
			@Param("storyId") Long storyId,
			@Param("viewerUserNo") Long viewerUserNo);

	int countView(
			@Param("storyId") Long storyId,
			@Param("viewerUserNo") Long viewerUserNo);

	List<StoryOwnerRow> findResidentOwnersWithActiveStories(@Param("viewerUserNo") Long viewerUserNo);

	int countUnviewedActiveByOwner(
			@Param("ownerUserNo") Long ownerUserNo,
			@Param("viewerUserNo") Long viewerUserNo);

	/** 나와 ACCEPTED 이웃 관계인지 (양방향) */
	int countAcceptedNeighborPair(
			@Param("userNoA") Long userNoA,
			@Param("userNoB") Long userNoB);
}
