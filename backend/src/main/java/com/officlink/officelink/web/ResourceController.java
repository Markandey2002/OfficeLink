package com.officlink.officelink.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.domain.ResourceType;
import com.officlink.officelink.dto.ResourceResponse;
import com.officlink.officelink.service.ResourceService;

@RestController
@RequestMapping("/api/resources")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ResourceController {

	private final ResourceService resourceService;

	public ResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@GetMapping
	public List<ResourceResponse> list(@RequestParam(required = false) ResourceType type) {
		return resourceService.list(type);
	}
}
