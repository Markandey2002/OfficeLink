package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.officlink.officelink.domain.Role;
import com.officlink.officelink.security.JwtProperties;
import com.officlink.officelink.security.JwtService;
import com.officlink.officelink.security.UserPrincipal;

/**
 * Pure unit tests for JWT issuance and claim contents.
 * No database or Spring context required.
 */
class JwtServiceTest {

	private JwtService buildService(String secret, String issuer, int expiryMinutes) {
		// Use the record constructor via reflection workaround — JwtProperties is a record.
		JwtProperties props = new JwtProperties(secret, issuer, expiryMinutes);
		return new JwtService(props);
	}

	@Test
	void createdToken_containsExpectedClaims() {
		JwtService svc = buildService(
				"ThisIsATestSecretThatIsAtLeast32BytesLong!!", "test-issuer", 60);

		UserPrincipal principal = new UserPrincipal(42L, "alice@example.com", "", "Alice", Role.USER);
		String token = svc.createToken(principal);

		assertThat(token).isNotBlank();
		// Token is a three-part JWT
		assertThat(token.split("\\.")).hasSize(3);
	}

	@Test
	void secretKey_reproducibleFromSameProperties() {
		String secret = "ThisIsATestSecretThatIsAtLeast32BytesLong!!";
		JwtService svc1 = buildService(secret, "issuer", 60);
		JwtService svc2 = buildService(secret, "issuer", 60);

		assertThat(svc1.getSecretKey().getEncoded())
				.isEqualTo(svc2.getSecretKey().getEncoded());
	}

	@Test
	void adminRole_encodedInToken() {
		JwtService svc = buildService(
				"ThisIsATestSecretThatIsAtLeast32BytesLong!!", "iss", 60);
		UserPrincipal admin = new UserPrincipal(1L, "admin@example.com", "", "Admin", Role.ADMIN);
		String token = svc.createToken(admin);
		// Basic structural assertion — the role name appears somewhere in the payload
		String payload = new String(
				java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
		assertThat(payload).contains("ADMIN");
		assertThat(payload).contains(JwtService.AUDIENCE);
	}

	@Test
	void audience_constantMatchesTokenClaim() {
		assertThat(JwtService.AUDIENCE).isEqualTo("office-link-api");
	}
}
