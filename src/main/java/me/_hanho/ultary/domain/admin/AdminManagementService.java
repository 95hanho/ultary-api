package me._hanho.ultary.domain.admin;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.common.validation.IdentityChangeCooldown;
import me._hanho.ultary.domain.admin.dto.response.*;
import me._hanho.ultary.domain.auth.TokenMapper;
import me._hanho.ultary.domain.feed.FeedService;
import me._hanho.ultary.domain.file.FileService;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;
import me._hanho.ultary.domain.pet.PetMapper;
import me._hanho.ultary.domain.pet.PetService;
import me._hanho.ultary.domain.story.StoryMapper;
import me._hanho.ultary.domain.user.UserMapper;
import me._hanho.ultary.domain.user.model.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagementService {
	private static final int TIMELINE_SOURCE_LIMIT = 30;
	private static final int TIMELINE_RESULT_LIMIT = 50;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final AdminManagementMapper mapper;
	private final FileService files;
	private final PetService pets;
	private final PetMapper petMapper;
	private final StoryMapper storyMapper;
	private final FeedService feedService;
	private final UserMapper userMapper;
	private final TokenMapper tokenMapper;
	private final PasswordEncoder passwordEncoder;

	public AdminPageResponse<AdminUserResponse> users(String q, String status, int page, int size) {
		validatePage(page, size);
		q = query(q);
		status = option(status, Set.of("ACTIVE", "SUSPENDED", "WITHDRAWN"));
		var rows = mapper.users(q, status, page * size, size);
		var summaries = files.findSummaries(rows.stream().map(AdminUserResponse::getProfileFileId)
				.filter(Objects::nonNull).toList());
		rows.forEach(row -> row.setProfileFile(summaries.get(row.getProfileFileId())));
		if (!rows.isEmpty()) {
			var names = new java.util.HashMap<Long, List<String>>();
			petMapper.findActiveMentions(rows.stream().map(AdminUserResponse::getUserNo).toList())
					.forEach(pet -> names.computeIfAbsent(pet.getUserNo(), key -> new ArrayList<>()).add(pet.getName()));
			rows.forEach(row -> row.setPetNames(names.getOrDefault(row.getUserNo(), List.of())));
		}
		return new AdminPageResponse<>(rows, mapper.countUsers(q, status), page, size);
	}

	public AdminUserDetailResponse user(Long id) {
		if (id == null || id <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		var row = mapper.user(id);
		if (row == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		row.setProfileFile(files.findSummary(row.getProfileFileId()));
		var userPets = pets.listByOwner(id);
		row.setPetNames(userPets.stream().map(pet -> pet.getName()).toList());
		return new AdminUserDetailResponse(row, userPets, mapper.feeds(id), mapper.reports(id));
	}

	public void assertProfileFile(Long userNo, Long fileId) {
		if (mapper.user(userNo) == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		if (mapper.countOwnedTimelineFile(userNo, fileId) <= 0)
			throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
	}

	public AdminPageResponse<AdminActionLogResponse> actions(String q, String kind, int page, int size) {
		validatePage(page, size);
		q = query(q);
		kind = option(kind, Set.of("USER", "TAG", "REPORT", "AI_REVIEW", "STORY", "FEED", "PET"));
		return new AdminPageResponse<>(mapper.actions(q, kind, page * size, size), mapper.countActions(q, kind), page,
				size);
	}

	public AdminTimelineResponse timeline(Long userNo) {
		if (userNo == null || userNo <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		if (mapper.user(userNo) == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

		List<AdminTimelineRow> rows = new ArrayList<>();
		rows.addAll(mapper.deletedStories(userNo, TIMELINE_SOURCE_LIMIT));
		rows.addAll(mapper.deletedFeeds(userNo, TIMELINE_SOURCE_LIMIT));
		rows.addAll(mapper.deletedPets(userNo, TIMELINE_SOURCE_LIMIT));
		var password = mapper.passwordChanged(userNo);
		if (password != null) rows.add(password);
		var nickname = mapper.nicknameChanged(userNo);
		if (nickname != null) rows.add(nickname);
		var withdrawn = mapper.accountWithdrawn(userNo);
		if (withdrawn != null) rows.add(withdrawn);

		rows.sort(Comparator
				.comparing(AdminTimelineRow::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(AdminTimelineRow::getId, Comparator.nullsLast(Comparator.reverseOrder())));

		List<AdminTimelineItemResponse> items = new ArrayList<>();
		for (AdminTimelineRow row : rows) {
			if (items.size() >= TIMELINE_RESULT_LIMIT) break;
			items.add(toItem(row));
		}
		return new AdminTimelineResponse(items);
	}

	/** 컨트롤러 execute 래퍼용. 삭제된 본인 스토리만 복구 */
	@Transactional
	public Void restoreDeletedStory(Long userNo, Long storyId) {
		requireUser(userNo);
		if (storyId == null || storyId <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		if (storyMapper.findDeletedByStoryIdAndUserNo(storyId, userNo) == null)
			throw new BusinessException(ErrorCode.STORY_NOT_FOUND);
		if (storyMapper.restoreByAdmin(storyId, userNo) == 0)
			throw new BusinessException(ErrorCode.STORY_NOT_FOUND);
		return null;
	}

	@Transactional
	public Void restoreDeletedFeed(Long userNo, Long feedId) {
		requireUser(userNo);
		if (feedId == null || feedId <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		Long owner = mapper.deletedFeedOwner(feedId);
		if (owner == null || !owner.equals(userNo)) throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
		feedService.restoreByAdmin(feedId);
		return null;
	}

	@Transactional
	public Void restoreDeletedPet(Long userNo, Long petId) {
		requireUser(userNo);
		if (petId == null || petId <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		if (petMapper.findDeletedByPetIdAndUserNo(petId, userNo) == null)
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		if (petMapper.restoreByAdmin(petId, userNo) == 0)
			throw new BusinessException(ErrorCode.PET_NOT_FOUND);
		return null;
	}

	/** @return 임시 평문 비밀번호(응답 1회용) */
	@Transactional
	public String resetPassword(Long userNo) {
		User user = requireUser(userNo);
		if ("WITHDRAWN".equals(user.getWithdrawalStatus()))
			throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
		String temporary = generateTemporaryPassword();
		if (userMapper.updatePasswordByAdmin(userNo, passwordEncoder.encode(temporary)) == 0)
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		tokenMapper.revokeAllByUserNo(userNo);
		return temporary;
	}

	@Transactional
	public Void clearNicknameCooldown(Long userNo) {
		User user = requireUser(userNo);
		if ("WITHDRAWN".equals(user.getWithdrawalStatus()))
			throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN);
		LocalDateTime cleared = LocalDateTime.now().minusDays(IdentityChangeCooldown.NICKNAME_DAYS + 1L);
		if (userMapper.updateNicknameChangedAt(userNo, cleared) == 0)
			throw new BusinessException(ErrorCode.USER_INACTIVE);
		return null;
	}

	@Transactional
	public Void cancelWithdrawal(Long userNo) {
		User user = requireUser(userNo);
		if (!"WITHDRAWN".equals(user.getWithdrawalStatus()))
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_WITHDRAWN);
		if (userMapper.reactivateWithdrawn(userNo) == 0)
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_WITHDRAWN);
		return null;
	}

	private User requireUser(Long userNo) {
		if (userNo == null || userNo <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
		User user = userMapper.findByUserNo(userNo);
		if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		return user;
	}

	private AdminTimelineItemResponse toItem(AdminTimelineRow row) {
		AdminTimelineItemResponse item = new AdminTimelineItemResponse();
		item.setType(row.getType());
		item.setId(row.getId());
		item.setOccurredAt(row.getOccurredAt());
		switch (row.getType()) {
			case "STORY_DELETED" -> {
				item.setPetNames(mapper.contentPetNames("STORY", row.getId()));
				item.setFiles(summaries(Stream.of(row.getThumbnailFileId(), row.getFileId())
						.filter(Objects::nonNull).distinct().toList()));
				item.setAction("RESTORE");
				item.setActionLabel("삭제 취소");
			}
			case "FEED_DELETED" -> {
				item.setPetNames(mapper.contentPetNames("FEED", row.getId()));
				item.setContentPreview(preview(row.getContent()));
				item.setFiles(summaries(mapper.feedMediaFileIds(row.getId())));
				item.setAction("RESTORE");
				item.setActionLabel("삭제 취소");
			}
			case "PET_DELETED" -> {
				item.setMessage("[" + row.getPetName() + "] pet이 삭제되었습니다");
				item.setPetNames(row.getPetName() == null ? List.of() : List.of(row.getPetName()));
				item.setFiles(summaries(row.getFileId() == null ? List.of() : List.of(row.getFileId())));
				item.setAction("RESTORE");
				item.setActionLabel("삭제 취소");
			}
			case "PASSWORD_CHANGED" -> {
				item.setMessage(row.getMessage());
				item.setAction("RESET_PASSWORD");
				item.setActionLabel("비밀번호 초기화");
			}
			case "NICKNAME_CHANGED" -> {
				item.setMessage(row.getMessage());
				item.setAction("CLEAR_COOLDOWN");
				item.setActionLabel("변경 제한 초기화");
			}
			case "ACCOUNT_WITHDRAWN" -> {
				item.setMessage(row.getMessage());
				item.setAction("CANCEL_WITHDRAWAL");
				item.setActionLabel("탈퇴 취소");
			}
			default -> {
			}
		}
		if (item.getPetNames() == null) item.setPetNames(List.of());
		if (item.getFiles() == null) item.setFiles(List.of());
		return item;
	}

	private List<FileSummaryResponse> summaries(List<Long> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) return List.of();
		var unique = new ArrayList<>(new LinkedHashSet<>(fileIds));
		var map = files.findSummaries(unique);
		List<FileSummaryResponse> out = new ArrayList<>();
		for (Long id : unique) {
			var summary = map.get(id);
			if (summary != null) out.add(summary);
		}
		return out;
	}

	private static String preview(String content) {
		if (content == null || content.isBlank()) return null;
		String trimmed = content.trim().replaceAll("\\s+", " ");
		if (trimmed.length() <= 15) return trimmed;
		return trimmed.substring(0, 15) + "…";
	}

	/** PasswordRules: 영문+숫자+특수, 8자 이상. 응답에만 평문 1회 포함 */
	static String generateTemporaryPassword() {
		String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
		String digits = "23456789";
		String specials = "!@#$%";
		StringBuilder sb = new StringBuilder();
		sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
		sb.append(digits.charAt(RANDOM.nextInt(digits.length())));
		sb.append(specials.charAt(RANDOM.nextInt(specials.length())));
		String pool = alphabet + digits;
		for (int i = 0; i < 9; i++) sb.append(pool.charAt(RANDOM.nextInt(pool.length())));
		char[] chars = sb.toString().toCharArray();
		for (int i = chars.length - 1; i > 0; i--) {
			int j = RANDOM.nextInt(i + 1);
			char tmp = chars[i];
			chars[i] = chars[j];
			chars[j] = tmp;
		}
		return new String(chars);
	}

	/** 조치 실패 또는 이력 저장 실패 시 상태 변경도 함께 롤백한다. */
	@Transactional
	public <T> T execute(Long adminNo, String action, String kind, Long id, Long requestId, String reason,
			Supplier<T> operation) {
		if (adminNo == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
		if (reason != null && reason.length() > 500) throw new BusinessException(ErrorCode.INVALID_INPUT);
		var before = mapper.target(kind, id, requestId);
		T result = operation.get();
		var after = mapper.target(kind, id, requestId);
		var entry = new AdminActionLogResponse();
		entry.setAdminNo(adminNo);
		entry.setAction(action);
		entry.setTargetType(kind);
		entry.setTargetId(id);
		entry.setRequestId(requestId);
		entry.setTargetLabel(after != null ? after.getTargetLabel() : before != null ? before.getTargetLabel() : null);
		entry.setBeforeStatus(before == null ? null : before.getAfterStatus());
		entry.setAfterStatus(after == null ? null : after.getAfterStatus());
		entry.setReason(reason == null || reason.isBlank() ? null : reason.trim());
		if (mapper.insertAction(entry) != 1) throw new BusinessException(ErrorCode.UNAUTHORIZED);
		return result;
	}

	private void validatePage(int page, int size) {
		if (page < 0 || page > 1000000 || size < 1 || size > 50)
			throw new BusinessException(ErrorCode.INVALID_INPUT);
	}

	private String query(String q) {
		if (q == null || q.isBlank()) return null;
		q = q.trim();
		if (q.startsWith("@")) q = q.substring(1);
		if (q.length() > 100) throw new BusinessException(ErrorCode.INVALID_INPUT);
		return q.isBlank() ? null : q;
	}

	private String option(String value, Set<String> allowed) {
		if (value == null || value.isBlank() || value.equals("ALL")) return null;
		if (!allowed.contains(value)) throw new BusinessException(ErrorCode.INVALID_INPUT);
		return value;
	}
}
