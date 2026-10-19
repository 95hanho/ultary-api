package me._hanho.ultary.domain.tag;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.tag.model.Tag;
import me._hanho.ultary.domain.tag.model.TagImage;
import me._hanho.ultary.domain.tag.model.TagProfileRequest;
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

	/** hashtag 정확 일치. use_count 높은 것 1건 */
	Tag findActiveByHashtag(@Param("hashtag") String hashtag);

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

	int insertProfileRequest(TagProfileRequest request);

	int updatePendingProfileRequest(TagProfileRequest request);

	int reviewProfileRequest(
			@Param("tagProfileRequestId") Long tagProfileRequestId,
			@Param("status") String status,
			@Param("adminNo") Long adminNo,
			@Param("rejectMessage") String rejectMessage);

	/** 최신 요청을 대기로. 거절 멘트와 검토 시각을 비운다 */
	int reopenProfileRequest(@Param("tagProfileRequestId") Long tagProfileRequestId);

	TagProfileRequest findPendingByTagId(@Param("tagId") Long tagId);

	/** 그 태그의 최신 설명 요청. 없으면 null */
	TagProfileRequest findLatestByTagId(@Param("tagId") Long tagId);

	TagProfileRequest findProfileRequestById(@Param("tagProfileRequestId") Long tagProfileRequestId);

	TagProfileRequest findLatestByTagAndRequester(
			@Param("tagId") Long tagId,
			@Param("requesterUserNo") Long requesterUserNo);

	List<TagProfileRequest> findProfileRequests(
			@Param("requesterUserNo") Long requesterUserNo,
			@Param("status") String status,
			@Param("cursorRequestId") Long cursorRequestId,
			@Param("limit") int limit);

	/** 관리자 목록. status가 없으면 전부. 커서와 limit는 세지 않는다 */
	int countProfileRequests(@Param("status") String status);

	int insertRequestImage(
			@Param("tagProfileRequestId") Long tagProfileRequestId,
			@Param("fileId") Long fileId,
			@Param("sortOrder") int sortOrder);

	int deleteRequestImages(@Param("tagProfileRequestId") Long tagProfileRequestId);

	List<Long> findRequestImageFileIds(@Param("tagProfileRequestId") Long tagProfileRequestId);

	int deleteImagesByTagId(@Param("tagId") Long tagId);

	int markCommercialRequested(@Param("tagId") Long tagId);

	int applyApprovedProfile(TagProfileRequest request);

	int markProfileRejected(@Param("tagId") Long tagId);

	/** 공개 중인 설명을 숨긴다. 제목·소개·링크·이미지는 지우지 않는다 */
	int hidePublishedProfile(@Param("tagId") Long tagId);
}
