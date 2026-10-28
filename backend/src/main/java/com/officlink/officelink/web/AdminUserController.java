package com.officlink.officelink.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.dto.UpdateRoleRequest;
import com.officlink.officelink.dto.UserResponse;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.service.UserAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final UserAdminService userAdminService;

	public AdminUserController(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	@GetMapping
	public List<UserResponse> list() {
		return userAdminService.listUsers();
	}

	@PutMapping("/{id}/role")
	public UserResponse updateRole(
			@PathVariable Long id,
			@Valid @RequestBody UpdateRoleRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return userAdminService.updateRole(id, request, principal);
	}
}
