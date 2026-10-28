package com.officlink.officelink.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.dto.CreateResourceRequest;
import com.officlink.officelink.dto.ResourceResponse;
import com.officlink.officelink.dto.UpdateResourceRequest;
import com.officlink.officelink.service.ResourceService;

import jakarta.validation.Valid;

/**
 * Admin-only resource management: create, update, and deactivate resources.
 * Employees use GET /api/resources (which only returns active ones).
 */
@RestController
@RequestMapping("/api/admin/resources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminResourceController {

	private final ResourceService resourceService;

	public AdminResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	/** List all resources (including inactive ones) for the admin view. */
	@GetMapping
	public List<ResourceResponse> listAll() {
		return resourceService.listAll();
	}

	/** Create a new resource. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResourceResponse create(@Valid @RequestBody CreateResourceRequest request) {
		return resourceService.create(request);
	}

	/**
	 * Update resource details or deactivate it.
	 * Set {@code active=false} to soft-delete (existing bookings are unaffected).
	 */
	@PutMapping("/{id}")
	public ResourceResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateResourceRequest request) {
		return resourceService.update(id, request);
	}
}
