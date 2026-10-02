package me._hanho.ultary.domain.activity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.domain.activity.dto.response.ActivityItemResponse;
import me._hanho.ultary.domain.activity.dto.response.ActivityListResponse;
import me._hanho.ultary.domain.activity.model.ActivityRow;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.security.principal.UserPrincipal;

@Service
@RequiredArgsConstructor
public class ActivityService {

	private static final int DEFAULT_LIMIT = 30;
	private static final int MAX_LIMIT = 50;

	private final ActivityMapper activityMapper;
	private final PetService petService;
	private final FileService fileService;

	@Transactional(readOnly = true)
	public ActivityListResponse list(UserPrincipal principal, Integer limit) {
		List<ActivityRow> rows = activityMapper.findByUser(principal.getUserNo(), resolveLimit(limit));
		return ActivityListResponse.builder()
				.items(toItems(rows))
				.build();
	}

	private List<ActivityItemResponse> toItems(List<ActivityRow> rows) {
		Set<Long> targetUserNos = new HashSet<>();
		for (ActivityRow row : rows) {
			if (row.getTargetUserNo() != null) {
				targetUserNos.add(row.getTargetUserNo());
			}
		}
		Map<Long, Integer> profileIds = petService.representativeProfileFileIds(targetUserNos);
		Set<Long> fileIds = new HashSet<>();
		for (Integer profileFileId : profileIds.values()) {
			if (profileFileId != null) {
				fileIds.add(profileFileId.longValue());
			}
		}
		Map<Long, FileSummaryResponse> files = fileService.findSummaries(fileIds);
		return rows.stream().map(row -> toItem(row, profileIds, files)).toList();
	}

	private ActivityItemResponse toItem(
			ActivityRow row,
			Map<Long, Integer> profileIds,
			Map<Long, FileSummaryResponse> files) {
		Integer profileFileId = profileIds.get(row.getTargetUserNo());
		Long profileId = profileFileId == null ? null : profileFileId.longValue();
		String snippet = row.getSnippet();
		if (snippet != null && snippet.isBlank()) {
			snippet = null;
		}
		return ActivityItemResponse.builder()
				.type(row.getActivityType())
				.occurredAt(row.getOccurredAt())
				.targetUserNo(row.getTargetUserNo())
				.targetNickname(row.getTargetNickname())
				.profileFileId(profileId)
				.profileFile(profileId == null ? null : files.get(profileId))
				.snippet(snippet)
				.feedId(row.getFeedId())
				.feedCommentId(row.getFeedCommentId())
				.feedReplyId(row.getFeedReplyId())
				.storyId(row.getStoryId())
				.neighborId(row.getNeighborId())
				.neighborStatus(row.getNeighborStatus())
				.build();
	}

	private int resolveLimit(Integer limit) {
		if (limit == null || limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
