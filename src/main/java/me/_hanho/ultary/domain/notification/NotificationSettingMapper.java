package me._hanho.ultary.domain.notification;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import me._hanho.ultary.domain.notification.model.NotificationSetting;

@Mapper
public interface NotificationSettingMapper {

	NotificationSetting findByUserNo(@Param("userNo") Long userNo);

	int upsert(NotificationSetting setting);
}
