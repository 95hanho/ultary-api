package me._hanho.ultary.domain.dm.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Getter
@Builder
public class DmShareTargetItemResponse {

	private Long userNo;
	private String nickname;
	/** 활성 펫의 mentionId. @ 없음. 펫 priority 순 */
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<String> petTags;
    private List<String> petNames;
	/** 개별 펫 선택용. priority 순 */
	private List<PetChoice> pets;
	public record PetChoice(Long petId, @com.fasterxml.jackson.annotation.JsonIgnore String mentionId, String name) {}
	private FileSummaryResponse profileFile;
	/** 이미 대화방이 있으면 그 번호. 없으면 null */
	private Long dmRoomId;
	/** 내가 상대를 팔로우 중 */
	private boolean following;
	/** 상대가 나를 팔로우 중 */
	private boolean followedBy;
	/**
	 * 1~3 DM/내 스토리 태그/내 피드 태그(순서 지정 가능), 4 나를 스토리 태그,
	 * 5 나를 피드 태그, 6 최근 팔로우, 7 최근 팔로워, 8 팔로우, 9 팔로워.
	 * 한 사람은 더 앞 그룹에만 있다.
	 */
	private int group;
	/** 그 그룹에서 정렬에 쓴 시각 */
	private LocalDateTime activeAt;
}
