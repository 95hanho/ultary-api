package me._hanho.ultary.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import me._hanho.ultary.security.handler.JwtAccessDeniedHandler;
import me._hanho.ultary.security.handler.JwtAuthenticationEntryPoint;
import me._hanho.ultary.security.jwt.JwtAuthenticationFilter;
import me._hanho.ultary.security.jwt.JwtProperties;
import me._hanho.ultary.security.bff.BffAuthenticationFilter;
import me._hanho.ultary.security.bff.BffSecurityProperties;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, BffSecurityProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final BffAuthenticationFilter bffAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(jwtAccessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/v1/health/**",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								"/api/v1/auth/logout",
								"/api/v1/auth/signup",
								"/api/v1/auth/nickname/check",
								"/api/v1/auth/phone",
								"/api/v1/auth/phone/verify",
								"/api/v1/auth/password",
								"/api/v1/auth/password/token",
								"/api/v1/auth/social/login",
								"/api/v1/admin/auth/login",
								"/api/v1/admin/auth/refresh",
								"/api/v1/admin/auth/logout",
								"/api/v1/test/**")
						.permitAll()
						// 공유 게시글 단건: PUBLIC은 비로그인 허용 (spring-auth-api.md)
						.requestMatchers(HttpMethod.GET, "/api/v1/feeds/*", "/api/v1/public/ultaries/*", "/api/v1/public/seo/sitemap", "/api/v1/files/*/content", "/api/v1/users/*/ultary-links", "/api/v1/users/*/pet-feeds").permitAll()
						// 소켓 핸드셰이크는 티켓 전. 인증은 연결 후 첫 메시지
						.requestMatchers("/api/v1/ws").permitAll()
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().permitAll())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(bffAuthenticationFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public FilterRegistrationBean<BffAuthenticationFilter> bffFilterRegistration() {
		var registration = new FilterRegistrationBean<>(bffAuthenticationFilter);
		registration.setEnabled(false); // SecurityFilterChain에서만 한 번 실행한다.
		return registration;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
}
