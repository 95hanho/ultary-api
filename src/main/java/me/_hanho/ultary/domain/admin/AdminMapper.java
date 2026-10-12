package me._hanho.ultary.domain.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.admin.model.Admin;

@Mapper
public interface AdminMapper {

	Admin findByLoginId(@Param("loginId") String loginId);

	Admin findByAdminNo(@Param("adminNo") Long adminNo);

	int updateLastLoginAt(@Param("adminNo") Long adminNo);
}
