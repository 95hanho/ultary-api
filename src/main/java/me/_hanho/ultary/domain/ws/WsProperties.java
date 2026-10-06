package me._hanho.ultary.domain.ws;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ultary.ws")
public class WsProperties {

	/** 브라우저 Origin. 소켓은 Spring 주소로 바로 붙는다 */
	private List<String> allowedOrigins = new ArrayList<>(List.of(
			"http://localhost:3000",
			"http://127.0.0.1:3000"));

	/** 입장 토큰 수명(초). 한 번만 쓸 수 있다 */
	private int ticketTtlSeconds = 30;
}
