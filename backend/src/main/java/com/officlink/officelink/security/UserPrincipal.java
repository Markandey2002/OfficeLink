package com.officlink.officelink.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Role;

public class UserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String passwordHash;
	private final String displayName;
	private final Role role;

	public UserPrincipal(Long id, String email, String passwordHash, String displayName, Role role) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	public static UserPrincipal from(AppUser user) {
		return new UserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getDisplayName(),
				user.getRole());
	}

	public Long getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
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
