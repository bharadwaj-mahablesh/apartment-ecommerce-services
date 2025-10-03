package com.community.ecommerce.apartmentservice.repository;

import com.community.ecommerce.apartmentservice.entity.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    Page<Apartment> findByApartmentNameContainingIgnoreCase(String name, Pageable pageable);
}
