package me._hanho.ultary.security.bff;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.bff")
public class BffSecurityProperties {

	@NotBlank
	@Size(min = 32, max = 256)
	private String sharedKey;
	private boolean requireHttps;
	/** TLS 종료 프록시의 실제 peer IP/CIDR만 지정한다. 기본은 전달 헤더를 신뢰하지 않는다. */
	private List<String> trustedProxies = new ArrayList<>();
}
