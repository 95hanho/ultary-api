package me._hanho.ultary.domain.settings;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.settings.model.UserPrivacy;

@Mapper
public interface PrivacyMapper {

	UserPrivacy findByUserNo(@Param("userNo") Long userNo);

	int upsert(UserPrivacy privacy);
}
