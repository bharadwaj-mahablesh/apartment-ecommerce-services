package com.community.ecommerce.apartmentservice.controller;

import com.community.ecommerce.apartmentservice.dto.ApartmentAddressDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentContactDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentRegistrationRequest;
import com.community.ecommerce.apartmentservice.dto.ApartmentResponse;
import com.community.ecommerce.apartmentservice.exception.ResourceNotFoundException;
import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import com.community.ecommerce.apartmentservice.service.ApartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApartmentController.class)
public class ApartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApartmentService apartmentService;

    @MockitoBean
    private ApartmentMapper apartmentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private ApartmentDomain apartmentDomain;
    private ApartmentResponse apartmentResponse;
    private ApartmentRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        ApartmentAddressDTO addressDTO = new ApartmentAddressDTO("Kanakapura Road", "", "Bengaluru", "Karnataka", "India", "560062");
        apartmentDomain = ApartmentDomain.builder().id(1L).apartmentName("Prestige Falcon City").build();
        apartmentResponse = new ApartmentResponse(1L, "Prestige Falcon City", addressDTO, List.of(new ApartmentContactDTO("Priya", "p@p.com", "123")), List.of("A"));
        registrationRequest = new ApartmentRegistrationRequest("Prestige Falcon City", addressDTO, List.of(new ApartmentContactDTO("Priya", "p@p.com", "123")), List.of("A"));
    }

    @Test
    void whenGetApartmentById_withValidId_thenReturns200_andApartment() throws Exception {
        // Arrange
        given(apartmentService.getApartmentById(1L)).willReturn(apartmentDomain);
        given(apartmentMapper.toResponse(apartmentDomain)).willReturn(apartmentResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/apartments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.apartmentName").value("Prestige Falcon City"));
    }

    @Test
    void whenGetApartmentById_withInvalidId_thenReturns404() throws Exception {
        // Arrange
        given(apartmentService.getApartmentById(99L)).willThrow(new ResourceNotFoundException("Not Found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/apartments/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenCreateApartment_withValidRequest_thenReturns201_andCreatedApartment() throws Exception {
        // Arrange
        given(apartmentService.saveApartment(any(ApartmentDomain.class))).willReturn(apartmentDomain);
        given(apartmentMapper.toDomain(any(ApartmentRegistrationRequest.class))).willReturn(ApartmentDomain.builder().build());
        given(apartmentMapper.toResponse(any(ApartmentDomain.class))).willReturn(apartmentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void whenCreateApartment_withInvalidRequest_thenReturns400() throws Exception {
        // Arrange
        ApartmentRegistrationRequest invalidRequest = new ApartmentRegistrationRequest("", null, List.of(), List.of());

        // Act & Assert
        mockMvc.perform(post("/api/v1/apartments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenUpdateApartment_withValidRequest_thenReturns200_andUpdatedApartment() throws Exception {
        // Arrange
        given(apartmentService.updateApartment(eq(1L), any(ApartmentDomain.class))).willReturn(apartmentDomain);
        given(apartmentMapper.toDomain(any(ApartmentRegistrationRequest.class))).willReturn(ApartmentDomain.builder().build());
        given(apartmentMapper.toResponse(any(ApartmentDomain.class))).willReturn(apartmentResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/apartments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apartmentName").value("Prestige Falcon City"));
    }

    @Test
    void whenDeleteApartment_withValidId_thenReturns204() throws Exception {
        // Arrange
        doNothing().when(apartmentService).deleteApartment(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/apartments/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
