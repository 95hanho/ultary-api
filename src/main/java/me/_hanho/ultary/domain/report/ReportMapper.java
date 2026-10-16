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

	/** 목록과 같은 대상 닉네임·본문·정지 여부 */
	Report findAdminById(@Param("reportId") Long reportId);

	Report findOpenByReporterAndTarget(
			@Param("reporterUserNo") Long reporterUserNo,
			@Param("targetType") String targetType,
			@Param("targetUserNo") Long targetUserNo,
			@Param("targetFeedId") Long targetFeedId,
			@Param("targetCommentId") Long targetCommentId,
			@Param("targetReplyId") Long targetReplyId);

	List<Report> findList(
			@Param("status") String status,
			@Param("cursorReportId") Long cursorReportId,
			@Param("limit") int limit);

	/** status가 없으면 전부. 커서와 limit는 세지 않는다 */
	int countList(@Param("status") String status);

	/** 신고요청·확인 건수. 메뉴 뱃지용 */
	int countOpen();

	int close(
			@Param("reportId") Long reportId,
			@Param("status") String status);

	/** 같은 대상·상태의 다른 신고 건수. 정지·삭제조치 원상복구 전에 본다 */
	int countOtherWithStatus(
			@Param("excludeReportId") Long excludeReportId,
			@Param("status") String status,
			@Param("targetType") String targetType,
			@Param("targetUserNo") Long targetUserNo,
			@Param("targetFeedId") Long targetFeedId,
			@Param("targetCommentId") Long targetCommentId,
			@Param("targetReplyId") Long targetReplyId);

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
