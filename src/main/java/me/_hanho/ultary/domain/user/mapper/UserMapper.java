package me._hanho.ultary.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.User;

@Mapper
public interface UserMapper {

	User findActiveByUserNo(@Param("userNo") Long userNo);

	User findByPhone(@Param("phone") String phone);

	User findByEmail(@Param("email") String email);

	int countByNickname(
			@Param("nickname") String nickname,
			@Param("excludeUserNo") Long excludeUserNo);

	int insert(User user);

	int updateProfile(User user);

	int updatePassword(
			@Param("userNo") Long userNo,
			@Param("password") String password);

	int updateWithdrawalStatus(
			@Param("userNo") Long userNo,
			@Param("withdrawalStatus") String withdrawalStatus);
}
