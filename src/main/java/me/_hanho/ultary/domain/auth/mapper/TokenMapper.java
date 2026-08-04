package me._hanho.ultary.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.auth.model.Token;

@Mapper
public interface TokenMapper {

	int insert(Token token);

	Token findValidUserRefreshToken(
			@Param("userNo") Long userNo,
			@Param("refreshToken") String refreshToken);

	int revokeByTokenId(@Param("tokenId") Long tokenId);

	int revokeAllByUserNo(@Param("userNo") Long userNo);
}
