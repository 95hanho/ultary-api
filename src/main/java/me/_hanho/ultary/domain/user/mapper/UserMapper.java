package me._hanho.ultary.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.user.model.User;

@Mapper
public interface UserMapper {

	User findByLoginId(@Param("loginId") String loginId);

	User findActiveByUserNo(@Param("userNo") Long userNo);
}
