package com.community.ecommerce.apartmentservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ApartmentBlocksDomain {
    private Long id;
    private String blockName;
    private LocalDateTime createdAt;
}
