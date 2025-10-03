package com.community.ecommerce.apartmentservice.model;

import com.community.ecommerce.apartmentservice.entity.ApartmentAddress;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class ApartmentDomain {
    private Long id;
    private String apartmentName;
    private ApartmentAddress apartmentAddress;
    private List<ApartmentBlocksDomain> apartmentBlocksDomainList;
    private List<ApartmentContactsDomain> apartmentContactsDomainList;
    private LocalDateTime createdAt;
}
