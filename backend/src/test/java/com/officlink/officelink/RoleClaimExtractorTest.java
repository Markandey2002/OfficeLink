package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.officlink.officelink.security.RoleClaimExtractor;
import com.officlink.officelink.domain.Role;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for role extraction from JWT claims.
 * Verifies that admin/user roles are correctly parsed from all supported claim shapes.
 */
class RoleClaimExtractorTest {

	private static Jwt jwt(Map<String, Object> claims) {
		return Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.claims(c -> c.putAll(claims))
				.build();
	}

	@Test
	void flatRolesClaim_ADMIN_returnsAdmin() {
		Jwt token = jwt(Map.of("roles", List.of("ADMIN")));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.ADMIN);
	}

	@Test
	void flatRolesClaim_USER_returnsUser() {
		Jwt token = jwt(Map.of("roles", List.of("USER")));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.USER);
	}

	@Test
	void realmAccess_ADMIN_returnsAdmin() {
		Jwt token = jwt(Map.of("realm_access", Map.of("roles", List.of("ADMIN"))));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.ADMIN);
	}

	@Test
	void prefixedRole_ROLE_ADMIN_returnsAdmin() {
		Jwt token = jwt(Map.of("roles", List.of("ROLE_ADMIN")));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.ADMIN);
	}

	@Test
	void noClaims_defaultsToUser() {
		Jwt token = jwt(Map.of());
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.USER);
	}

	@Test
	void unknownRole_defaultsToUser() {
		Jwt token = jwt(Map.of("roles", List.of("SUPERUSER", "MANAGER")));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.USER);
	}

	@Test
	void auth0NamespacedClaim_ADMIN_returnsAdmin() {
		Jwt token = jwt(Map.of("https://office-link/roles", List.of("ADMIN")));
		assertThat(RoleClaimExtractor.extractPrimaryRole(token)).isEqualTo(Role.ADMIN);
	}
}
