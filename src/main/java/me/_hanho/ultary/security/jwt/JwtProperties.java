package me._hanho.ultary.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	private Secret secret = new Secret();
	private Expiration expiration = new Expiration();

	@Getter
	@Setter
	public static class Secret {
		private String user;
		private String refresh;
		private String admin;
		private String phoneauth;
		private String phoneauthComplete;
		private String pwdchange;
	}

	@Getter
	@Setter
	public static class Expiration {
		private long access;
		private long refresh;
		private long phoneauth;
		private long phoneauthComplete;
		private long pwdchange;
	}
}
