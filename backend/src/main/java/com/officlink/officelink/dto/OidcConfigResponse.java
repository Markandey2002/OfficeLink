package com.officlink.officelink.dto;

public record OidcConfigResponse(
		boolean enabled,
		String issuerUri,
		String authority,
		String clientId,
		String redirectUri,
		String scope) {
}
