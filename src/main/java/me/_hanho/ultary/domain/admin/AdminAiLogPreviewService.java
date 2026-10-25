package me._hanho.ultary.domain.admin;

import java.util.ArrayList;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.ai.AiRequestLogMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.report.dto.response.ReportFeedPreviewResponse;
import me._hanho.ultary.domain.tag.TagService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiLogPreviewService {
    private final AiRequestLogMapper logs;
    private final FeedService feeds;
    private final TagService tags;

    public ReportFeedPreviewResponse preview(Long aiRequestId) {
        var log = logs.findForAdmin(aiRequestId);
        if (log == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (log.getTargetId() == null || !Set.of("FEED", "COMMENT", "REPLY").contains(log.getTargetType() == null ? "" : log.getTargetType())) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }
        var feed = feeds.getAiLogFeed(aiRequestId);
        var explanations = feed.getTagIds().stream().flatMap(id -> {
            try { return java.util.stream.Stream.of(tags.getDetail(id, null)); }
            catch (BusinessException error) {
                if (error.getErrorCode() == ErrorCode.TAG_NOT_FOUND) return java.util.stream.Stream.empty();
                throw error;
            }
        }).toList();
        return new ReportFeedPreviewResponse(feed, feeds.getAiLogComments(aiRequestId, feed.getFeedId()), explanations);
    }

    public void assertPreviewFile(Long aiRequestId, Long fileId) {
        var content = preview(aiRequestId);
        var files = new ArrayList<FileSummaryResponse>();
        files.add(content.feed().getAuthorProfileFile());
        content.feed().getMedia().forEach(media -> { files.add(media.getFile()); files.add(media.getThumbnailFile()); });
        content.comments().forEach(comment -> {
            files.add(comment.getAuthorProfileFile());
            comment.getReplies().forEach(reply -> files.add(reply.getAuthorProfileFile()));
        });
        content.tags().forEach(tag -> files.addAll(tag.getImages()));
        if (files.stream().noneMatch(file -> file != null && fileId.equals(file.getFileId()))) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }
}
