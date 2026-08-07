package me._hanho.ultary.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.UserSocial;

@Mapper
public interface UserSocialMapper {

	UserSocial findByProviderAndProviderUserId(
			@Param("provider") String provider,
			@Param("providerUserId") String providerUserId);

	UserSocial findByUserNoAndProvider(
			@Param("userNo") Long userNo,
			@Param("provider") String provider);

	int countByUserNo(@Param("userNo") Long userNo);

	int insert(UserSocial userSocial);

	int deleteByUserNoAndProvider(
			@Param("userNo") Long userNo,
			@Param("provider") String provider);
}
