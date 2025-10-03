package com.community.ecommerce.apartmentservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ApartmentAddressDTO(
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String cityName,
        @NotBlank String stateName,
        @NotBlank String countryName,
        @NotBlank String zipCode
) {}
