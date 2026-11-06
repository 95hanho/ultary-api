package me._hanho.ultary.security.principal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AdminPrincipal implements UserDetails {

	public static final String ROLE_SUPER = "SUPER";
	public static final String ROLE_OPERATOR = "OPERATOR";

	private final Long adminNo;
	private final String loginId;
	/** SUPER | OPERATOR */
	private final String role;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authorities = new ArrayList<>(2);
		authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		String normalized = normalizeRole(role);
		if (ROLE_SUPER.equals(normalized)) {
			authorities.add(new SimpleGrantedAuthority("ROLE_SUPER"));
		} else {
			authorities.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));
		}
		return authorities;
	}

	public boolean isSuper() {
		return ROLE_SUPER.equals(normalizeRole(role));
	}

	private static String normalizeRole(String value) {
		if (value == null || value.isBlank()) {
			return ROLE_OPERATOR;
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return loginId != null ? loginId : String.valueOf(adminNo);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
