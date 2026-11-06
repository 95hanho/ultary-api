package me._hanho.ultary.domain.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.User;

@Mapper
public interface UserMapper {
    @org.apache.ibatis.annotations.Select("SELECT user_no FROM ultary_user WHERE user_no=#{userNo} AND withdrawal_status='ACTIVE' FOR UPDATE")
    Long lockActiveUser(@Param("userNo") Long userNo);
    List<User> findAllByPhoneForLogin(@Param("phone") String phone);
    List<User> findAllByEmailForLogin(@Param("email") String email);


	User findActiveByUserNo(@Param("userNo") Long userNo);

	User findActiveByNickname(@Param("nickname") String nickname);

	/**
	 * 직전 닉네임태그로 활성 유저 조회.
	 * {@code nickname_changed_at}이 {@code reserveDays}일 이내일 때만.
	 */
	User findActiveByPreviousNicknameWithinDays(
			@Param("nickname") String nickname,
			@Param("reserveDays") int reserveDays);

	/** 탈퇴 포함. 로그인에서 탈퇴 계정을 구분할 때 */
	User findByUserNo(@Param("userNo") Long userNo);

	List<User> findActiveByUserNos(@Param("userNos") Collection<Long> userNos);

	User findByPhone(@Param("phone") String phone);

	User findByEmail(@Param("email") String email);

	/** 탈퇴 계정 포함. 비밀번호 로그인 식별용 */
	User findByPhoneForLogin(@Param("phone") String phone);

	User findByEmailForLogin(@Param("email") String email);

	/**
	 * 현재 닉네임 또는 변경 후 {@code reserveDays}일 이내 직전 닉네임과 중복이면 1 이상.
	 */
	int countByNickname(
			@Param("nickname") String nickname,
			@Param("excludeUserNo") Long excludeUserNo,
			@Param("reserveDays") int reserveDays);

	int countByEmail(
			@Param("email") String email,
			@Param("excludeUserNo") Long excludeUserNo);

	List<User> searchActiveByNickname(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);

	/** 닉네임태그 또는 펫 이름이 맞는 보호자. 유저당 한 줄 */
	List<User> searchActiveByPetMention(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);

	int insert(User user);

	int updateProfile(User user);

	int updateNickname(
			@Param("userNo") Long userNo,
			@Param("nickname") String nickname);

	/** local 테스트: 닉네임태그 쿨다운 기준 시각만 바꾼다 */
	int updateNicknameChangedAt(
			@Param("userNo") Long userNo,
			@Param("nicknameChangedAt") LocalDateTime nicknameChangedAt);

	int updatePassword(
			@Param("userNo") Long userNo,
			@Param("password") String password);

	/** 관리자 초기화. ACTIVE·SUSPENDED만. password_changed_at=NULL, must_change_password=1 */
	int updatePasswordByAdmin(
			@Param("userNo") Long userNo,
			@Param("password") String password);

	int suspend(@Param("userNo") Long userNo);

	int unsuspend(@Param("userNo") Long userNo);

	int updateWithdrawalStatus(
			@Param("userNo") Long userNo,
			@Param("withdrawalStatus") String withdrawalStatus);

	/** 탈퇴 취소. WITHDRAWN → ACTIVE, completed_at·suspended_at 비움 */
	int reactivateWithdrawn(@Param("userNo") Long userNo);

	int updateBio(
			@Param("userNo") Long userNo,
			@Param("bio") String bio,
			@Param("clearBio") boolean clearBio);
}
