package me._hanho.ultary.domain.ai;

import org.apache.ibatis.annotations.Mapper;

import me._hanho.ultary.domain.ai.model.AiRequestLog;

@Mapper
public interface AiRequestLogMapper {

	int insert(AiRequestLog log);
}
