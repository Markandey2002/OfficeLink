package com.officlink.officelink.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.ResourceType;
import com.officlink.officelink.dto.ResourceResponse;
import com.officlink.officelink.repository.ResourceRepository;

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
}
