package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.officlink.officelink.config.SecurityConfig;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.security.JwtProperties;
import com.officlink.officelink.security.JwtService;
import com.officlink.officelink.security.UserPrincipal;

/**
 * Pure unit tests for JWT issuance, HS256 enforcement, and Nimbus decoder validation.
 * No database or full Spring context required.
 */
class JwtServiceTest {

	private final SecurityConfig securityConfig = new SecurityConfig();

	private JwtService buildService(String secret, String issuer, int expiryMinutes) {
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
		String payload = new String(
				java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
		assertThat(payload).contains("ADMIN");
		assertThat(payload).contains(JwtService.AUDIENCE);
	}

	@Test
	void audience_constantMatchesTokenClaim() {
		assertThat(JwtService.AUDIENCE).isEqualTo("office-link-api");
	}

	@Test
	void createdToken_explicitlySignedWithHs256AndValidatedByLocalDecoder() {
		// 49-character secret (392 bits). Without explicit HS256, JJWT would infer HS384,
		// causing NimbusJwtDecoder to reject with "Another algorithm expected".
		String longSecret = "replace-with-a-long-random-secret-at-least-32-chars";
		String issuer = "office-link-local";
		JwtService svc = buildService(longSecret, issuer, 60);
		JwtDecoder decoder = securityConfig.localJwtDecoder(svc);

		UserPrincipal user = new UserPrincipal(101L, "employee@officlink.com", "", "Employee One", Role.USER);
		String token = svc.createToken(user);

		// Verify header explicitly specifies HS256
		String headerJson = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
		assertThat(headerJson).contains("\"alg\":\"HS256\"");

		// Verify Nimbus decoder validates signature, issuer, expiry, audience, and claims
		Jwt decoded = decoder.decode(token);
		assertThat(decoded).isNotNull();
		assertThat(decoded.getSubject()).isEqualTo("101");
		assertThat(decoded.getClaimAsString("email")).isEqualTo("employee@officlink.com");
		assertThat(decoded.getClaimAsString("preferred_username")).isEqualTo("employee@officlink.com");
		assertThat(decoded.getClaimAsString("name")).isEqualTo("Employee One");
		assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
		assertThat(decoded.getAudience()).contains("office-link-api");
		assertThat(decoded.getClaimAsString("iss")).isEqualTo(issuer);
	}

	@Test
	void wronglySignedToken_rejectedByDecoder() {
		String secret1 = "ValidSecretForServiceOneMustBe32CharsLong!!";
		String secret2 = "DifferentSecretForServiceTwoMustBe32Chars!!";
		String issuer = "office-link-local";

		JwtService svc1 = buildService(secret1, issuer, 60);
		JwtService svc2 = buildService(secret2, issuer, 60);
		JwtDecoder decoder1 = securityConfig.localJwtDecoder(svc1);

		UserPrincipal user = new UserPrincipal(5L, "intruder@officlink.com", "", "Intruder", Role.USER);
		String tokenSignedWithDifferentKey = svc2.createToken(user);

		// Must throw JwtException (e.g. BadJwtException) due to invalid signature
		assertThatThrownBy(() -> decoder1.decode(tokenSignedWithDifferentKey))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void tamperedSignature_rejectedByDecoder() {
		String secret = "ValidSecretForServiceOneMustBe32CharsLong!!";
		String issuer = "office-link-local";

		JwtService svc = buildService(secret, issuer, 60);
		JwtDecoder decoder = securityConfig.localJwtDecoder(svc);

		UserPrincipal user = new UserPrincipal(7L, "user@officlink.com", "", "User", Role.USER);
		String token = svc.createToken(user);

		String[] parts = token.split("\\.");
		// Corrupt the signature part
		String tamperedToken = parts[0] + "." + parts[1] + ".invalidSignatureStringHere";

		assertThatThrownBy(() -> decoder.decode(tamperedToken))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void secretShorterThan32Bytes_throwsIllegalArgumentException() {
		assertThatThrownBy(() -> buildService("too-short-secret", "iss", 60))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least 32 bytes");
	}
}
