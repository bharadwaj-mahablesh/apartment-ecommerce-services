package com.community.ecommerce.userservice;

import com.community.ecommerce.userservice.repository.RoleRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@TestPropertySource(properties = {"apartment.service.url=http://localhost:8081"})
class UserManagementServiceApplicationTests {

	@MockitoBean
	private RestTemplate restTemplate;

	@MockitoBean
	private RoleRepository roleRepository;

//	@Test
//	void contextLoads() {
//		// Mock the behavior of roleRepository if needed for context loading
//		// For example, if a bean depends on roleRepository.findByName during startup
//		// when(roleRepository.findByName("RESIDENT")).thenReturn(Optional.of(new Role(1L, "RESIDENT")));
//	}

}
