package me._hanho.ultary.domain.story;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.story.model.Story;
import me._hanho.ultary.domain.story.model.StoryLike;
import me._hanho.ultary.domain.story.model.StoryMentionRow;
import me._hanho.ultary.domain.story.model.StoryOwnerRow;
import me._hanho.ultary.domain.story.model.StoryText;

@Mapper
public interface StoryMapper {

	int insert(Story story);

	int insertText(StoryText text);

	int insertMention(StoryMentionRow mention);

	List<StoryText> findTextsByStoryId(@Param("storyId") Long storyId);

	List<StoryMentionRow> findMentionsByStoryId(@Param("storyId") Long storyId);

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

	/** local 테스트: 내 스토리 읽음 전부 삭제 */
	int deleteViewsByViewerUserNo(@Param("viewerUserNo") Long viewerUserNo);

	List<StoryOwnerRow> findResidentOwnersWithActiveStories(@Param("viewerUserNo") Long viewerUserNo);

	int countUnviewedActiveByOwner(
			@Param("ownerUserNo") Long ownerUserNo,
			@Param("viewerUserNo") Long viewerUserNo);

	/** 나와 ACCEPTED 이웃 관계인지 (양방향) */
	int countAcceptedNeighborPair(
			@Param("userNoA") Long userNoA,
			@Param("userNoB") Long userNoB);

	StoryLike findLike(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);

	int insertLike(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);

	int restoreLike(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);

	int softDeleteLike(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);

	int countActiveLikes(@Param("storyId") Long storyId);

	int countActiveLikeByUser(
			@Param("storyId") Long storyId,
			@Param("userNo") Long userNo);
}
