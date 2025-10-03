package com.community.ecommerce.apartmentservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ApartmentContactsDomain {
    private Long id;
    private String contactName;
    private String contactEmail;
    private String contactPhoneNumber;
    private LocalDateTime createdAt;
}
