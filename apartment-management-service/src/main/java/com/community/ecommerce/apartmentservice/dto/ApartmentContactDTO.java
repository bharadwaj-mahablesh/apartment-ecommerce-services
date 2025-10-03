package com.community.ecommerce.apartmentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ApartmentContactDTO(
        @NotBlank String name,
        @Email String emailAddress,
        @NotBlank @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits") String phoneNumber
) {}
