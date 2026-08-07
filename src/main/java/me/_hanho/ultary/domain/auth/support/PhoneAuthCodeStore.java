package me._hanho.ultary.domain.auth.support;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 휴대폰 인증번호 임시 저장 (단일 인스턴스용).
 * 운영에서는 Redis 등으로 교체한다.
 */
@Component
public class PhoneAuthCodeStore {

	private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

	public void save(String phone, String code, long ttlSeconds) {
		store.put(phone, new CodeEntry(code, Instant.now().plusSeconds(ttlSeconds)));
	}

	public boolean matches(String phone, String code) {
		CodeEntry entry = store.get(phone);
		if (entry == null) {
			return false;
		}
		if (Instant.now().isAfter(entry.expiresAt())) {
			store.remove(phone);
			return false;
		}
		return entry.code().equals(code);
	}

	public void remove(String phone) {
		store.remove(phone);
	}

	private record CodeEntry(String code, Instant expiresAt) {
	}
}
