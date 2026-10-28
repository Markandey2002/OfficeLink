package com.officlink.officelink.dto;

import com.officlink.officelink.domain.Role;

public record AuthResponse(
		String accessToken,
		String tokenType,
		Long userId,
		String email,
		String displayName,
		Role role) {
}
