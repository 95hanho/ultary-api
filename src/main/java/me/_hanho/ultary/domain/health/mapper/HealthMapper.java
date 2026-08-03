package me._hanho.ultary.domain.health.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthMapper {

	Integer ping();
}
