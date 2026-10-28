package com.officlink.officelink.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.officlink.officelink.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmailIgnoreCase(String email);

	Optional<AppUser> findByOidcSub(String oidcSub);

	boolean existsByEmailIgnoreCase(String email);
}
