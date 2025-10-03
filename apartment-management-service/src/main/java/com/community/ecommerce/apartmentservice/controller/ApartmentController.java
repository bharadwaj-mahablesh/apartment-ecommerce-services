package com.community.ecommerce.apartmentservice.controller;

import com.community.ecommerce.apartmentservice.dto.ApartmentRegistrationRequest;
import com.community.ecommerce.apartmentservice.dto.ApartmentResponse;
import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import com.community.ecommerce.apartmentservice.service.ApartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Apartment Management", description = "APIs for managing apartment listings")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final ApartmentMapper apartmentMapper;

    @Operation(summary = "Register a new apartment",
               description = "Registers a new apartment listing.",
               responses = {
                   @ApiResponse(responseCode = "201", description = "Apartment registered successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid input")
               })
    @PostMapping("/api/v1/apartments")
    public ResponseEntity<ApartmentResponse> createApartment(
            @Valid @RequestBody ApartmentRegistrationRequest apartmentRegistrationRequest) {
        ApartmentDomain apartmentDomain = apartmentMapper.toDomain(apartmentRegistrationRequest);
        ApartmentDomain savedApartmentDomain = apartmentService.saveApartment(apartmentDomain);
        return ResponseEntity.status(HttpStatus.CREATED).body(apartmentMapper.toResponse(savedApartmentDomain));
    }

    @Operation(summary = "Get all apartments",
               description = "Retrieve a paginated and sortable list of all apartment listings.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "List of apartments retrieved")
               })
    @GetMapping("/api/v1/apartments")
    public ResponseEntity<Page<ApartmentResponse>> getApartments(Pageable pageable) {
        Page<ApartmentDomain> apartmentDomainPage = apartmentService.getAllApartments(pageable);
        return ResponseEntity.ok(apartmentDomainPage.map(apartmentMapper::toResponse));
    }

    @Operation(summary = "Get apartment by ID",
               description = "Retrieve an apartment listing by its ID.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Apartment found"),
                   @ApiResponse(responseCode = "404", description = "Apartment not found")
               })
    @GetMapping("/api/v1/apartments/{id}")
    public ResponseEntity<ApartmentResponse> getApartmentById(@PathVariable Long id) {
        ApartmentDomain apartmentDomain = apartmentService.getApartmentById(id);
        return ResponseEntity.ok(apartmentMapper.toResponse(apartmentDomain));
    }

    @Operation(summary = "Search apartments by name",
               description = "Search for apartment listings by name with pagination and sorting.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "List of apartments retrieved")
               })
    @GetMapping("/api/v1/apartments/search")
    public ResponseEntity<Page<ApartmentResponse>> searchApartmentsByName(@RequestParam String name, Pageable pageable) {
        Page<ApartmentDomain> apartmentDomainPage = apartmentService.searchApartmentsByName(name, pageable);
        return ResponseEntity.ok(apartmentDomainPage.map(apartmentMapper::toResponse));
    }

    @Operation(summary = "Update an apartment",
               description = "Update an existing apartment listing by ID.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Apartment updated successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid input"),
                   @ApiResponse(responseCode = "404", description = "Apartment not found")
               })
    @PutMapping("/api/v1/apartments/{id}")
    public ResponseEntity<ApartmentResponse> updateApartment(@PathVariable Long id,
            @Valid @RequestBody ApartmentRegistrationRequest apartmentRegistrationRequest) {
        ApartmentDomain apartmentDomain = apartmentMapper.toDomain(apartmentRegistrationRequest);
        ApartmentDomain updatedApartment = apartmentService.updateApartment(id, apartmentDomain);
        return ResponseEntity.ok(apartmentMapper.toResponse(updatedApartment));
    }

    @Operation(summary = "Delete an apartment",
               description = "Delete an apartment listing by ID.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Apartment deleted successfully"),
                   @ApiResponse(responseCode = "404", description = "Apartment not found")
               })
    @DeleteMapping("/api/v1/apartments/{id}")
    public ResponseEntity<Void> deleteApartment(@PathVariable Long id) {
        apartmentService.deleteApartment(id);
        return ResponseEntity.noContent().build();
    }
}
