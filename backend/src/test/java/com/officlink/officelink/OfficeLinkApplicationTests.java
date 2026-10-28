package com.officlink.officelink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:postgresql://localhost:5432/office_link",
		"spring.datasource.username=postgres",
		"spring.datasource.password=5189"
})
class OfficeLinkApplicationTests {

	@Test
	void contextLoads() {
	}
}
