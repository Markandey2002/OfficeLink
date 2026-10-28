package com.officlink.officelink.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Resource;
import com.officlink.officelink.domain.ResourceType;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.repository.ResourceRepository;

@Configuration
public class DataSeeder {

	@Bean
	CommandLineRunner seedData(
			AppUserRepository userRepository,
			ResourceRepository resourceRepository,
			PasswordEncoder passwordEncoder) {
		return args -> {
			if (!userRepository.existsByEmailIgnoreCase("admin@officlink.com")) {
				AppUser admin = new AppUser();
				admin.setEmail("admin@officlink.com");
				admin.setDisplayName("Office Admin");
				admin.setRole(Role.ADMIN);
				admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
				userRepository.save(admin);
			}

			if (!userRepository.existsByEmailIgnoreCase("user@officlink.com")) {
				AppUser user = new AppUser();
				user.setEmail("user@officlink.com");
				user.setDisplayName("Alex Employee");
				user.setRole(Role.USER);
				user.setPasswordHash(passwordEncoder.encode("User123!"));
				userRepository.save(user);
			}

			if (resourceRepository.count() == 0) {
				resourceRepository.save(resource("Room A", ResourceType.MEETING_ROOM, "Floor 2"));
				resourceRepository.save(resource("Room B", ResourceType.MEETING_ROOM, "Floor 3"));
				resourceRepository.save(resource("Desk 12", ResourceType.DESK, "Floor 1 - Open Area"));
				resourceRepository.save(resource("Desk 14", ResourceType.DESK, "Floor 1 - Quiet Zone"));
				resourceRepository.save(resource("Projector P1", ResourceType.EQUIPMENT, "AV Closet"));
				resourceRepository.save(resource("Laptop Cart", ResourceType.EQUIPMENT, "IT Desk"));
			}
		};
	}

	private Resource resource(String name, ResourceType type, String location) {
		Resource resource = new Resource();
		resource.setName(name);
		resource.setType(type);
		resource.setLocation(location);
		resource.setActive(true);
		return resource;
	}
}
