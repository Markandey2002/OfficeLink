package com.officlink.officelink.dto;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Role;

public record UserResponse(
		Long id,
		String email,
		String displayName,
		Role role) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole());
	}
}
