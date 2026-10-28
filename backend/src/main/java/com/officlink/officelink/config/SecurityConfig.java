package com.officlink.officelink.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.officlink.officelink.security.JwtAuthConverter;
import com.officlink.officelink.security.JwtProperties;
import com.officlink.officelink.security.JwtService;
import com.officlink.officelink.security.OidcProperties;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ JwtProperties.class, OidcProperties.class })
public class SecurityConfig {

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtDecoder localJwtDecoder(JwtService jwtService) {
		SecretKey key = jwtService.getSecretKey();
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// Validate issuer + expiry (via createDefaultWithIssuer) AND the audience claim.
		JwtClaimValidator<Collection<String>> audienceValidator =
				new JwtClaimValidator<>(JwtClaimNames.AUD,
						aud -> aud != null && aud.contains(JwtService.AUDIENCE));
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(jwtService.getIssuer()),
				audienceValidator);
		decoder.setJwtValidator(validator);
		return decoder;
	}

	@Bean
	AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
			JwtDecoder localJwtDecoder,
			JwtService jwtService,
			OidcProperties oidcProperties,
			JwtAuthConverter jwtAuthConverter) {

		Map<String, JwtDecoder> decoders = new LinkedHashMap<>();
		decoders.put(jwtService.getIssuer(), localJwtDecoder);

		if (oidcProperties.enabled() && oidcProperties.issuerUri() != null && !oidcProperties.issuerUri().isBlank()) {
			JwtDecoder oidcDecoder = JwtDecoders.fromIssuerLocation(oidcProperties.issuerUri());
			decoders.put(oidcProperties.issuerUri(), oidcDecoder);
		}

		Map<String, org.springframework.security.authentication.AuthenticationManager> managers = new LinkedHashMap<>();
		decoders.forEach((issuer, decoder) -> {
			JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
			provider.setJwtAuthenticationConverter(jwtAuthConverter);
			managers.put(issuer, provider::authenticate);
		});

		return new JwtIssuerAuthenticationManagerResolver(managers::get);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/oidc-config").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver));

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		config.setAllowedOrigins(origins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
