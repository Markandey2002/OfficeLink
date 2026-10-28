package com.officlink.officelink.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;

import com.officlink.officelink.domain.Role;

/**
 * Extracts Admin/User roles from JWT claims only (never from the client).
 * Supports local tokens (`roles`) and Keycloak/Auth0-style (`realm_access.roles`).
 */
public final class RoleClaimExtractor {

	private RoleClaimExtractor() {
	}

	public static Role extractPrimaryRole(Jwt jwt) {
		Set<String> roles = extractRawRoles(jwt);
		if (roles.stream().anyMatch(r -> equalsRole(r, "ADMIN"))) {
			return Role.ADMIN;
		}
		return Role.USER;
	}

	public static Set<String> extractRawRoles(Jwt jwt) {
		Set<String> roles = new HashSet<>();

		Object rolesClaim = jwt.getClaim("roles");
		addRoles(roles, rolesClaim);

		Map<String, Object> realmAccess = jwt.getClaim("realm_access");
		if (realmAccess != null) {
			addRoles(roles, realmAccess.get("roles"));
		}

		// Auth0-style custom namespace claim support
		Object appMeta = jwt.getClaim("https://office-link/roles");
		addRoles(roles, appMeta);

		return roles;
	}

	private static void addRoles(Set<String> target, Object value) {
		if (value instanceof Collection<?> collection) {
			for (Object item : collection) {
				if (item != null) {
					target.add(item.toString());
				}
			}
		} else if (value instanceof String str) {
			target.add(str);
		}
	}

	private static boolean equalsRole(String raw, String expected) {
		String normalized = raw.trim().toUpperCase();
		return normalized.equals(expected)
				|| normalized.equals("ROLE_" + expected)
				|| normalized.equals("OFFICE-LINK-" + expected)
				|| normalized.equals("OFFICELINK_" + expected);
	}

	public static List<String> asAuthorityNames(Role role) {
		return List.of("ROLE_" + role.name());
	}
}
