package com.officlink.officelink.dto;

import com.officlink.officelink.domain.Resource;
import com.officlink.officelink.domain.ResourceType;

public record ResourceResponse(
		Long id,
		String name,
		ResourceType type,
		String location,
		boolean active) {

	public static ResourceResponse from(Resource resource) {
		return new ResourceResponse(
				resource.getId(),
				resource.getName(),
				resource.getType(),
				resource.getLocation(),
				resource.isActive());
	}
}
