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

	List<User> findActiveByUserNos(@Param("userNos") Collection<Long> userNos);

	User findByPhone(@Param("phone") String phone);

	User findByEmail(@Param("email") String email);

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

	int updateWithdrawalStatus(
			@Param("userNo") Long userNo,
			@Param("withdrawalStatus") String withdrawalStatus);

	int updateBio(
			@Param("userNo") Long userNo,
			@Param("bio") String bio,
			@Param("clearBio") boolean clearBio);
}
