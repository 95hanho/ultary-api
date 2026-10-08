package me._hanho.ultary.domain.report;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.report.model.Report;

@Mapper
public interface ReportMapper {

	int insert(Report report);

	Report findById(@Param("reportId") Long reportId);

	Report findOpenByReporterAndTarget(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetType") String targetType,
			@Param("targetUserNo") Long targetUserNo,
			@Param("targetFeedId") Long targetFeedId,
			@Param("targetCommentId") Long targetCommentId,
			@Param("targetReplyId") Long targetReplyId);

	List<Report> findList(
			@Param("status") String status,
			@Param("limit") int limit);

	int close(
			@Param("reportId") Long reportId,
			@Param("status") String status);

	int confirm(@Param("reportId") Long reportId);

	List<Report> findMineByFeedIds(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetIds") Collection<Long> targetIds);

	List<Report> findMineByCommentIds(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetIds") Collection<Long> targetIds);

	List<Report> findMineByReplyIds(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetIds") Collection<Long> targetIds);

	List<Report> findMineByUserNos(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetIds") Collection<Long> targetIds);

	int deleteOwnRequested(
			@Param("reportId") Long reportId,
			@Param("reporterUserNo") Long reporterUserNo);
}
