package com.community.ecommerce.userservice.controller;

import com.community.ecommerce.common.events.UserStatus;
import com.community.ecommerce.userservice.dto.*;
import com.community.ecommerce.userservice.entity.Role;
import com.community.ecommerce.userservice.repository.RoleRepository;
import com.community.ecommerce.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Sql("/data.sql") // Execute data.sql before tests to pre-populate roles
public class UserControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }

    @BeforeEach
    void setUp() {
        // Mock the apartment service URL property
        // This is usually done via @TestPropertySource or by mocking Environment, but for simplicity
        // and given @Value is used, we ensure restTemplate is mocked for its calls.
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistrationFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Register a new user
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Alice", "Smith", "alice.smith@example.com", "securePass123", 1L, "Block A", "101");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.roleName").value("PENDING_USER"));
    }

    @Test
    void testUserRetrievalFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Create an ADMIN user to perform approval
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        // Manually update admin user's role in DB for test (since no admin endpoint yet)
        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        // 2. Login as ADMIN to get JWT
        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Register a new user
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Alice", "Smith", "alice.smith@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 4. Get user by ID
        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));

        // 5. Get all users with pagination
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminJwt)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2)) // admin and new user
                .andExpect(jsonPath("$.content[0].email").value("admin@example.com"));
    }

    @Test
    void registerUser_invalidInput_returnsBadRequest() throws Exception {
        UserRegistrationRequest invalidRequest = new UserRegistrationRequest(
                "", "", "invalid-email", "short", null, "", ""); // Invalid data

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void registerUser_apartmentNotFound_returnsNotFound() throws Exception {
        // Mock apartment service to throw NOT_FOUND
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Alice", "Smith", "alice.smith@example.com", "securePass123", 99L, "Block A", "101");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Apartment with ID 99 not found. Your apartment is not onboarded yet."));
    }

    @Test
    void registerUser_blockNotFound_returnsNotFound() throws Exception {
        // Mock apartment service response for valid apartment but invalid block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block B", "Block C"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Alice", "Smith", "alice.smith@example.com", "securePass123", 1L, "Block A", "101");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Block 'Block A' not found in Apartment with ID 1."));
    }

    @Test
    void getUserById_notFound_returnsNotFound() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Create an ADMIN user to perform the request
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Attempt to get a non-existent user as ADMIN
        mockMvc.perform(get("/api/v1/users/{id}", 99L)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: 99"));
    }

    @Test
    void testUpdateUserFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Create an ADMIN user to perform the update
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Register a new user
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Bob", "Johnson", "bob.johnson@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 3. Update the user as ADMIN
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "Robert", "Johnson", "robert.johnson@example.com", "Block B", "202");

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Robert"))
                .andExpect(jsonPath("$.email").value("robert.johnson@example.com"))
                .andExpect(jsonPath("$.blockName").value("Block B"));
    }

    @Test
    void testDeleteUserFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Create an ADMIN user to perform the deletion
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Register a new user
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Charlie", "Brown", "charlie.brown@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 3. Delete the user as ADMIN
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNoContent());

        // 4. Verify deletion
        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testChangePasswordFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Create an ADMIN user to perform the password change
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Register a new user
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "David", "Lee", "david.lee@example.com", "oldPassword123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 3. Change password as ADMIN
        PasswordChangeRequest changePasswordRequest = new PasswordChangeRequest("oldPassword123", "newPassword456");

        mockMvc.perform(put("/api/v1/users/{id}/password", userId)
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isNoContent());

        // Note: Verifying password change would require re-authenticating, which is outside the scope of this direct test.
    }

    @Test
    void testApproveUserFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Register a new user (PENDING_USER)
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Eve", "Adams", "eve.adams@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 2. Create an ADMIN user to perform approval
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin", "User", "admin@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        // Manually update admin user's role in DB for test (since no admin endpoint yet)
        userRepository.findByEmail("admin@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        // 3. Login as ADMIN to get JWT
        LoginRequest adminLoginRequest = new LoginRequest("admin@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 4. Approve the user as ADMIN
        UserStatusUpdateRequest approveRequest = new UserStatusUpdateRequest(UserStatus.APPROVED, "RESIDENT");
        mockMvc.perform(put("/api/v1/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.roleName").value("RESIDENT"));

        // 5. Verify user status and role are updated in DB
        userRepository.findById(userId).ifPresent(user -> {
            assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);
            assertThat(user.getRole().getName()).isEqualTo("RESIDENT");
        });
    }

    @Test
    void testRejectUserFlow() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Register a new user (PENDING_USER)
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Frank", "Green", "frank.green@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 2. Create an ADMIN user to perform rejection
        UserRegistrationRequest adminRegisterRequest = new UserRegistrationRequest(
                "Admin2", "User2", "admin2@example.com", "adminPass123", 1L, "Block A", "101");
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegisterRequest)))
                .andExpect(status().isCreated());

        // Manually update admin user's role in DB for test
        userRepository.findByEmail("admin2@example.com").ifPresent(adminUser -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            adminUser.setRole(adminRole);
            userRepository.save(adminUser);
        });

        // 3. Login as ADMIN to get JWT
        LoginRequest adminLoginRequest = new LoginRequest("admin2@example.com", "adminPass123");
        MvcResult adminLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 4. Reject the user as ADMIN
        UserStatusUpdateRequest rejectRequest = new UserStatusUpdateRequest(UserStatus.REJECTED, null);
        mockMvc.perform(put("/api/v1/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.roleName").value("PENDING_USER")); // Role should remain PENDING_USER or original

        // 5. Verify user status is updated in DB
        userRepository.findById(userId).ifPresent(user -> {
            assertThat(user.getStatus()).isEqualTo(UserStatus.REJECTED);
        });
    }

    @Test
    void testApproveUser_unauthorized() throws Exception {
        // Mock apartment service response for valid apartment and block
        ApartmentDetailsResponse validApartment = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(validApartment);

        // 1. Register a new user (PENDING_USER)
        UserRegistrationRequest registerRequest = new UserRegistrationRequest(
                "Grace", "Hopper", "grace.hopper@example.com", "securePass123", 1L, "Block A", "101");

        MvcResult createResult = mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long userId = createResponse.get("id").asLong();

        // 2. Login as the PENDING_USER to get JWT
        LoginRequest userLoginRequest = new LoginRequest("grace.hopper@example.com", "securePass123");
        MvcResult userLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String userJwt = objectMapper.readTree(userLoginResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Attempt to approve user as PENDING_USER (should be forbidden)
        UserStatusUpdateRequest approveRequest = new UserStatusUpdateRequest(UserStatus.APPROVED, "RESIDENT");
        mockMvc.perform(put("/api/v1/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isForbidden());
    }
}
