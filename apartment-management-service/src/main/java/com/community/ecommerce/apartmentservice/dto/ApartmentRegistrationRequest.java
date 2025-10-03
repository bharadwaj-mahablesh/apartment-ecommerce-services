package com.community.ecommerce.apartmentservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApartmentRegistrationRequest(
        @NotBlank String apartmentName,
        @Valid ApartmentAddressDTO apartmentAddress,
        @NotEmpty List<ApartmentContactDTO> apartmentPointOfContacts,
        @NotEmpty List<String> blockNames
) {}
