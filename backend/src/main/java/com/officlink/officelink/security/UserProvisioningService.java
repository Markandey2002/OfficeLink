package com.officlink.officelink.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.web.error.ApiException;

/**
 * Maps a validated JWT to a local AppUser. Role always comes from JWT claims.
 */
@Service
public class UserProvisioningService {

	private final AppUserRepository userRepository;

	public UserProvisioningService(AppUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public UserPrincipal resolvePrincipal(Jwt jwt) {
		Role roleFromJwt = RoleClaimExtractor.extractPrimaryRole(jwt);
		String email = firstNonBlank(
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("preferred_username"));
		String name = firstNonBlank(
				jwt.getClaimAsString("name"),
				jwt.getClaimAsString("preferred_username"),
				email);
		String sub = jwt.getSubject();
		String issuer = jwt.getClaimAsString("iss");

		// Local tokens use numeric subject = app user id
		if (issuer != null && issuer.contains("office-link-local")) {
			Long userId = Long.valueOf(sub);
			AppUser user = userRepository.findById(userId)
					.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found for token"));
			// Prefer JWT role claim over DB so RBAC is claim-driven
			return new UserPrincipal(user.getId(), user.getEmail(), "", user.getDisplayName(), roleFromJwt);
		}

		if (email == null || email.isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "OIDC token is missing email claim");
		}

		AppUser user = userRepository.findByOidcSub(sub)
				.or(() -> userRepository.findByEmailIgnoreCase(email))
				.orElseGet(AppUser::new);

		user.setEmail(email.trim().toLowerCase());
		user.setDisplayName(name != null ? name : email);
		user.setOidcSub(sub);
		user.setRole(roleFromJwt);
		if (user.getPasswordHash() == null) {
			user.setPasswordHash(null);
		}
		user = userRepository.save(user);

		return new UserPrincipal(user.getId(), user.getEmail(), "", user.getDisplayName(), roleFromJwt);
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
