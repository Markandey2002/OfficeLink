package com.officlink.officelink.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.Role;
import com.officlink.officelink.dto.UpdateRoleRequest;
import com.officlink.officelink.dto.UserResponse;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.web.error.ApiException;

@Service
public class UserAdminService {

	private final AppUserRepository userRepository;

	public UserAdminService(AppUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> listUsers() {
		return userRepository.findAll().stream()
				.sorted((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail()))
				.map(UserResponse::from)
				.toList();
	}

	@Transactional
	public UserResponse updateRole(Long userId, UpdateRoleRequest request, UserPrincipal actor) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

		if (user.getId().equals(actor.getId()) && request.role() != Role.ADMIN) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot demote themselves");
		}

		user.setRole(request.role());
		return UserResponse.from(userRepository.save(user));
	}
}
