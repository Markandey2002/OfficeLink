package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.security.UserProvisioningService;

class UserProvisioningServiceTest {

	private AppUserRepository userRepository;
	private UserProvisioningService provisioningService;

	@BeforeEach
	void setUp() {
		userRepository = mock(AppUserRepository.class);
		provisioningService = new UserProvisioningService(userRepository);
	}

	@Test
	void resolvePrincipal_nonUrlStringIssuer_processedWithoutConversionException() {
		// Non-URL string issuer "office-link-local" would throw IllegalArgumentException
		// if jwt.getIssuer() (which expects a URL) were called.
		Jwt jwt = Jwt.withTokenValue("mock-token")
				.header("alg", "HS256")
				.claim("iss", "office-link-local")
				.subject("42")
				.claim("email", "alex@officlink.com")
				.claim("preferred_username", "alex@officlink.com")
				.claim("name", "Alex Developer")
				.claim("roles", List.of("USER"))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();

		AppUser user = new AppUser();
		user.setId(42L);
		user.setEmail("alex@officlink.com");
		user.setDisplayName("Alex Developer");
		user.setRole(Role.USER);

		when(userRepository.findById(42L)).thenReturn(Optional.of(user));

		assertThatCode(() -> {
			UserPrincipal principal = provisioningService.resolvePrincipal(jwt);
			assertThat(principal).isNotNull();
			assertThat(principal.getId()).isEqualTo(42L);
			assertThat(principal.getUsername()).isEqualTo("alex@officlink.com");
			assertThat(principal.getRole()).isEqualTo(Role.USER);
		}).doesNotThrowAnyException();
	}

	@Test
	void resolvePrincipal_oidcTokenWithUrlIssuer_provisionsSuccessfully() {
		Jwt jwt = Jwt.withTokenValue("mock-oidc-token")
				.header("alg", "RS256")
				.claim("iss", "http://keycloak:8081/realms/office-link")
				.subject("keycloak-sub-12345")
				.claim("email", "oidcuser@officlink.com")
				.claim("name", "OIDC User")
				.claim("realm_access", Map.of("roles", List.of("ADMIN")))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();

		when(userRepository.findByOidcSub("keycloak-sub-12345")).thenReturn(Optional.empty());
		when(userRepository.findByEmailIgnoreCase("oidcuser@officlink.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
			AppUser u = invocation.getArgument(0);
			u.setId(99L);
			return u;
		});

		UserPrincipal principal = provisioningService.resolvePrincipal(jwt);
		assertThat(principal).isNotNull();
		assertThat(principal.getId()).isEqualTo(99L);
		assertThat(principal.getUsername()).isEqualTo("oidcuser@officlink.com");
		assertThat(principal.getRole()).isEqualTo(Role.ADMIN);
	}
}
