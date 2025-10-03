package com.community.ecommerce.apartmentservice.controller;

import com.community.ecommerce.apartmentservice.dto.ApartmentAddressDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentContactDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentRegistrationRequest;
import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import com.community.ecommerce.apartmentservice.repository.ApartmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class ApartmentControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private ApartmentMapper apartmentMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterEach
    void tearDown() {
        apartmentRepository.deleteAll();
    }

    @Test
    void testApartmentCRUDFlow() throws Exception {
        // 1. CREATE an Apartment
        ApartmentRegistrationRequest createRequest = new ApartmentRegistrationRequest(
                "Prestige Falcon City",
                new ApartmentAddressDTO("Kanakapura Road", "", "Bengaluru", "Karnataka", "India", "560062"),
                List.of(new ApartmentContactDTO("Priya Sharma", "priya.sharma@example.com", "+91 9876543210")),
                List.of("Tower A", "Tower B")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.apartmentName").value("Prestige Falcon City"))
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long apartmentId = createResponse.get("id").asLong();

        // 2. READ the Apartment by ID
        mockMvc.perform(get("/api/v1/apartments/{id}", apartmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(apartmentId))
                .andExpect(jsonPath("$.apartmentName").value("Prestige Falcon City"));

        // 3. SEARCH for the Apartment with pagination
        mockMvc.perform(get("/api/v1/apartments/search")
                        .param("name", "Falcon")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].apartmentName").value("Prestige Falcon City"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 4. UPDATE the Apartment
        ApartmentRegistrationRequest updateRequest = new ApartmentRegistrationRequest(
                "Prestige Falcon City - Updated",
                new ApartmentAddressDTO("Kanakapura Main Road", "", "Bengaluru", "Karnataka", "India", "560062"),
                List.of(new ApartmentContactDTO("Priya S.", "priya.s@example.com", "+91 9876543211")),
                List.of("Tower A", "Tower B", "Tower C")
        );

        mockMvc.perform(put("/api/v1/apartments/{id}", apartmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentName").value("Prestige Falcon City - Updated"))
                .andExpect(jsonPath("$.contacts[0].name").value("Priya S."));

        // 5. DELETE the Apartment
        mockMvc.perform(delete("/api/v1/apartments/{id}", apartmentId))
                .andExpect(status().isNoContent());

        // 6. VERIFY Deletion
        mockMvc.perform(get("/api/v1/apartments/{id}", apartmentId))
                .andExpect(status().isNotFound());
    }

    private ApartmentRegistrationRequest createApartmentRequest(String name, String city) {
        return new ApartmentRegistrationRequest(
                name,
                new ApartmentAddressDTO("Street", "", city, "State", "Country", "12345"),
                List.of(new ApartmentContactDTO("Contact", "contact@example.com", "1234567890")),
                List.of("Block A")
        );
    }

    @Test
    void testGetAllApartmentsWithPaginationAndSorting() throws Exception {
        // Arrange: Create multiple apartments
        apartmentRepository.save(apartmentMapper.toEntity(apartmentMapper.toDomain(createApartmentRequest("Apartment C", "City A"))));
        apartmentRepository.save(apartmentMapper.toEntity(apartmentMapper.toDomain(createApartmentRequest("Apartment A", "City B"))));
        apartmentRepository.save(apartmentMapper.toEntity(apartmentMapper.toDomain(createApartmentRequest("Apartment B", "City C"))));

        // Act & Assert: Get all apartments with pagination and sorting by apartmentName ascending
        mockMvc.perform(get("/api/v1/apartments")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "apartmentName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].apartmentName").value("Apartment A"))
                .andExpect(jsonPath("$.content[1].apartmentName").value("Apartment B"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Act & Assert: Get all apartments with pagination and sorting by apartmentName descending, second page
        mockMvc.perform(get("/api/v1/apartments")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "apartmentName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].apartmentName").value("Apartment A"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
