package com.officlink.officelink.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.officlink.officelink.domain.Resource;
import com.officlink.officelink.domain.ResourceType;

import jakarta.persistence.LockModeType;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

	List<Resource> findByActiveTrueOrderByNameAsc();

	List<Resource> findByActiveTrueAndTypeOrderByNameAsc(ResourceType type);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Resource r where r.id = :id")
	Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
