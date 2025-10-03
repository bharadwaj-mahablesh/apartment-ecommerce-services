package com.community.ecommerce.apartmentservice.dto;

import java.util.List;

public record ApartmentResponse(
        Long id,
        String apartmentName,
        ApartmentAddressDTO address,
        List<ApartmentContactDTO> contacts,
        List<String> blockNames
) {}
