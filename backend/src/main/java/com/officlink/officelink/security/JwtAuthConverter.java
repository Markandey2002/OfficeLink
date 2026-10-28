package com.officlink.officelink.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UserProvisioningService userProvisioningService;

	public JwtAuthConverter(UserProvisioningService userProvisioningService) {
		this.userProvisioningService = userProvisioningService;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UserPrincipal principal = userProvisioningService.resolvePrincipal(jwt);
		return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
	}
}
