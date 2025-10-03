package com.community.ecommerce.apartmentservice.service.impl;

import com.community.ecommerce.apartmentservice.entity.Apartment;
import com.community.ecommerce.apartmentservice.exception.ResourceNotFoundException;
import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import com.community.ecommerce.apartmentservice.repository.ApartmentRepository;
import com.community.ecommerce.apartmentservice.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentMapper apartmentMapper;

    @Override
    public Page<ApartmentDomain> getAllApartments(Pageable pageable) {
        return apartmentRepository.findAll(pageable)
                .map(apartmentMapper::toDomain);
    }

    @Override
    public ApartmentDomain saveApartment(ApartmentDomain apartmentDomain) {
        Apartment apartment = apartmentMapper.toEntity(apartmentDomain);
        // Set bidirectional relationships
        apartment.getApartmentBlocks().forEach(block -> block.setApartment(apartment));
        apartment.getApartmentContacts().forEach(contact -> contact.setApartment(apartment));

        Apartment savedApartment = apartmentRepository.save(apartment);
        return apartmentMapper.toDomain(savedApartment);
    }

    @Override
    public ApartmentDomain getApartmentById(Long id) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found with id: " + id));
        return apartmentMapper.toDomain(apartment);
    }

    @Override
    public Page<ApartmentDomain> searchApartmentsByName(String name, Pageable pageable) {
        return apartmentRepository.findByApartmentNameContainingIgnoreCase(name, pageable)
                .map(apartmentMapper::toDomain);
    }

    @Override
    public ApartmentDomain updateApartment(Long id, ApartmentDomain apartmentDomain) {
        Apartment existingApartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found with id: " + id));

        // Use the mapper to update the existing entity from the domain object
        apartmentMapper.updateEntity(apartmentDomain, existingApartment);

        // Set bidirectional relationships
        existingApartment.getApartmentBlocks().forEach(block -> block.setApartment(existingApartment));
        existingApartment.getApartmentContacts().forEach(contact -> contact.setApartment(existingApartment));

        Apartment updatedApartment = apartmentRepository.save(existingApartment);
        return apartmentMapper.toDomain(updatedApartment);
    }

    @Override
    public void deleteApartment(Long id) {
        if (!apartmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Apartment not found with id: " + id);
        }
        // Note: Consider soft delete in the future to avoid orphaned residents.
        apartmentRepository.deleteById(id);
    }
}
