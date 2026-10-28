package com.officlink.officelink.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oidc")
public record OidcProperties(
		boolean enabled,
		String issuerUri,
		String audience,
		String clientId,
		String redirectUri) {
}
