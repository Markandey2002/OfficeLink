package com.officlink.officelink.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.Resource;
import com.officlink.officelink.domain.ResourceType;
import com.officlink.officelink.dto.CreateResourceRequest;
import com.officlink.officelink.dto.ResourceResponse;
import com.officlink.officelink.dto.UpdateResourceRequest;
import com.officlink.officelink.repository.ResourceRepository;
import com.officlink.officelink.web.error.ApiException;

@Service
public class ResourceService {

	private final ResourceRepository resourceRepository;

	public ResourceService(ResourceRepository resourceRepository) {
		this.resourceRepository = resourceRepository;
	}

	@Transactional(readOnly = true)
	public List<ResourceResponse> list(ResourceType type) {
		var resources = type == null
				? resourceRepository.findByActiveTrueOrderByNameAsc()
				: resourceRepository.findByActiveTrueAndTypeOrderByNameAsc(type);
		return resources.stream().map(ResourceResponse::from).toList();
	}

	/** Admin: list all resources (including inactive). */
	@Transactional(readOnly = true)
	public List<ResourceResponse> listAll() {
		return resourceRepository.findAll().stream()
				.sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
				.map(ResourceResponse::from)
				.toList();
	}

	/** Admin: create a new resource. */
	@Transactional
	public ResourceResponse create(CreateResourceRequest request) {
		if (resourceRepository.findAll().stream()
				.anyMatch(r -> r.getName().equalsIgnoreCase(request.name()))) {
			throw new ApiException(HttpStatus.CONFLICT, "A resource with that name already exists");
		}
		Resource resource = new Resource();
		resource.setName(request.name().trim());
		resource.setType(request.type());
		resource.setLocation(request.location());
		resource.setActive(true);
		return ResourceResponse.from(resourceRepository.save(resource));
	}

	/** Admin: update resource details or deactivate it. */
	@Transactional
	public ResourceResponse update(Long id, UpdateResourceRequest request) {
		Resource resource = resourceRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));
		resource.setName(request.name().trim());
		resource.setType(request.type());
		resource.setLocation(request.location());
		resource.setActive(Boolean.TRUE.equals(request.active()));
		return ResourceResponse.from(resourceRepository.save(resource));
	}
}
