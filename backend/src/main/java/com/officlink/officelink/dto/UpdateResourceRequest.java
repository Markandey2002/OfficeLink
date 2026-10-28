package com.officlink.officelink.dto;

import com.officlink.officelink.domain.ResourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
		@NotBlank @Size(max = 120) String name,
		@NotNull ResourceType type,
		@Size(max = 255) String location,
		@NotNull Boolean active) {
}
