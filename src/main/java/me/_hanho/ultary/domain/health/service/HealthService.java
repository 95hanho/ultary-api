package me._hanho.ultary.domain.health.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me._hanho.ultary.common.exception.BusinessException;
import me._hanho.ultary.common.exception.ErrorCode;
import me._hanho.ultary.domain.health.mapper.HealthMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

	private final HealthMapper healthMapper;

	public String checkDbConnection() {
		try {
			Integer result = healthMapper.ping();
			if (result == null || result != 1) {
				throw new BusinessException(ErrorCode.DB_CONNECTION_FAILED);
			}
			return "DB_OK";
		} catch (BusinessException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Database health check failed", ex);
			throw new BusinessException(ErrorCode.DB_CONNECTION_FAILED);
		}
	}
}
