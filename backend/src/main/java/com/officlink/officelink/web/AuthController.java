package com.officlink.officelink.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.dto.AuthResponse;
import com.officlink.officelink.dto.LoginRequest;
import com.officlink.officelink.dto.OidcConfigResponse;
import com.officlink.officelink.dto.RegisterRequest;
import com.officlink.officelink.security.OidcProperties;
import com.officlink.officelink.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final OidcProperties oidcProperties;

	public AuthController(AuthService authService, OidcProperties oidcProperties) {
		this.authService = authService;
		this.oidcProperties = oidcProperties;
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/oidc-config")
	public OidcConfigResponse oidcConfig() {
		String issuer = oidcProperties.issuerUri();
		return new OidcConfigResponse(
				oidcProperties.enabled(),
				issuer,
				issuer,
				oidcProperties.clientId(),
				oidcProperties.redirectUri(),
				"openid profile email");
	}
}
