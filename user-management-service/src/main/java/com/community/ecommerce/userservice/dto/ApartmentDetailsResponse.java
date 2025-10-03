package com.community.ecommerce.userservice.dto;

import java.util.List;

public record ApartmentDetailsResponse(
        Long id,
        List<String> blockNames
) {}
