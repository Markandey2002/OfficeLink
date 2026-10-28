package com.officlink.officelink.dto;

import com.officlink.officelink.domain.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
