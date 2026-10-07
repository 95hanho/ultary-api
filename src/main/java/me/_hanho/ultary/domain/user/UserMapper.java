package me._hanho.ultary.domain.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.User;

@Mapper
public interface UserMapper {

	User findActiveByUserNo(@Param("userNo") Long userNo);

	/** 탈퇴 포함. 로그인에서 탈퇴 계정을 구분할 때 */
	User findByUserNo(@Param("userNo") Long userNo);

	List<User> findActiveByUserNos(@Param("userNos") Collection<Long> userNos);

	User findByPhone(@Param("phone") String phone);

	User findByEmail(@Param("email") String email);

	/** 탈퇴 계정 포함. 비밀번호 로그인 식별용 */
	User findByPhoneForLogin(@Param("phone") String phone);

	User findByEmailForLogin(@Param("email") String email);

	int countByNickname(
			@Param("nickname") String nickname,
			@Param("excludeUserNo") Long excludeUserNo);

	int countByEmail(
			@Param("email") String email,
			@Param("excludeUserNo") Long excludeUserNo);

	List<User> searchActiveByNickname(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);

	/** 펫 mention_id가 맞는 보호자. 유저당 한 줄 */
	List<User> searchActiveByPetMention(
			@Param("viewerUserNo") Long viewerUserNo,
			@Param("q") String q,
			@Param("limit") int limit);

	int insert(User user);

	int updateProfile(User user);

	int updateNickname(
			@Param("userNo") Long userNo,
			@Param("nickname") String nickname);

	/** local 테스트: 닉네임 쿨다운 기준 시각만 바꾼다 */
	int updateNicknameChangedAt(
			@Param("userNo") Long userNo,
			@Param("nicknameChangedAt") LocalDateTime nicknameChangedAt);

	int updatePassword(
			@Param("userNo") Long userNo,
			@Param("password") String password);

	int suspend(@Param("userNo") Long userNo);

	int unsuspend(@Param("userNo") Long userNo);

	int updateWithdrawalStatus(
			@Param("userNo") Long userNo,
			@Param("withdrawalStatus") String withdrawalStatus);

	int updateBio(
			@Param("userNo") Long userNo,
			@Param("bio") String bio,
			@Param("clearBio") boolean clearBio);
}
