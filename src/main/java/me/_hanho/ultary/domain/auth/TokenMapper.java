package me._hanho.ultary.domain.auth;

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

	Token findValidAdminRefreshToken(
			@Param("adminNo") Long adminNo,
			@Param("refreshToken") String refreshToken);

	int revokeAllByAdminNo(@Param("adminNo") Long adminNo);

	int revokeUserSession(@Param("userNo") Long userNo, @Param("sessionId") String sessionId);

	int revokeAdminSession(@Param("adminNo") Long adminNo, @Param("sessionId") String sessionId);

	Long lockUserForRefresh(@Param("userNo") Long userNo);

	Long lockAdminForRefresh(@Param("adminNo") Long adminNo);

	Token findUserTokenForUpdate(@Param("userNo") Long userNo, @Param("refreshToken") String refreshToken);

	Token findAdminTokenForUpdate(@Param("adminNo") Long adminNo, @Param("refreshToken") String refreshToken);

	Token findTokenByIdForUpdate(@Param("tokenId") Long tokenId);

	int completeRotation(@Param("tokenId") Long tokenId, @Param("replacementId") Long replacementId);
}
