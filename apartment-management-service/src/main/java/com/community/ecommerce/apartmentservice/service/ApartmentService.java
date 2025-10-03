package com.community.ecommerce.apartmentservice.service;

import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApartmentService {
    Page<ApartmentDomain> getAllApartments(Pageable pageable);

    ApartmentDomain saveApartment(ApartmentDomain apartmentDomain);

    ApartmentDomain getApartmentById(Long id);

    Page<ApartmentDomain> searchApartmentsByName(String name, Pageable pageable);

    ApartmentDomain updateApartment(Long id, ApartmentDomain apartmentDomain);

    void deleteApartment(Long id);
}
