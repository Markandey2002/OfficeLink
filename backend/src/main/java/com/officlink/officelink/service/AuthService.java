package com.officlink.officelink.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.dto.AuthResponse;
import com.officlink.officelink.dto.LoginRequest;
import com.officlink.officelink.dto.RegisterRequest;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.security.JwtService;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.web.error.ApiException;

@Service
public class AuthService {

	private final AppUserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			AppUserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
		}

		AppUser user = new AppUser();
		user.setEmail(email);
		user.setDisplayName(request.displayName().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.USER);
		userRepository.save(user);

		return toAuthResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		var user = userRepository.findByEmailIgnoreCase(request.email().trim())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

		if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED,
					"This account uses OIDC/SSO only. Sign in with Keycloak.");
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		return toAuthResponse(user);
	}

	private AuthResponse toAuthResponse(AppUser user) {
		UserPrincipal principal = UserPrincipal.from(user);
		String token = jwtService.createToken(principal);
		return new AuthResponse(
				token,
				"Bearer",
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole());
	}
}
