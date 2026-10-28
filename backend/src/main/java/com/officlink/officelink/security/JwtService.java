package com.officlink.officelink.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues local OIDC-shaped JWTs (iss/sub/aud/roles/realm_access) for register/login.
 * Validation is performed by Spring Security OAuth2 Resource Server (NimbusJwtDecoder).
 */
@Service
public class JwtService {

	public static final String AUDIENCE = "office-link-api";

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public SecretKey getSecretKey() {
		return key;
	}

	public String getIssuer() {
		return properties.issuer();
	}

	public String createToken(UserPrincipal principal) {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(properties.expirationMinutes() * 60);
		String role = principal.getRole().name();

		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(principal.getId()))
				.audience().add(AUDIENCE).and()
				.claim("email", principal.getUsername())
				.claim("preferred_username", principal.getUsername())
				.claim("name", principal.getDisplayName())
				.claim("roles", List.of(role))
				// Keycloak-compatible claim shape so RBAC converters stay consistent
				.claim("realm_access", Map.of("roles", List.of(role)))
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(key)
				.compact();
	}
}
