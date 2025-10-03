package com.community.ecommerce.apartmentservice.service.impl;

import com.community.ecommerce.apartmentservice.entity.Apartment;
import com.community.ecommerce.apartmentservice.entity.ApartmentAddress;
import com.community.ecommerce.apartmentservice.exception.ResourceNotFoundException;
import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import com.community.ecommerce.apartmentservice.repository.ApartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApartmentServiceImplTest {

    @Mock
    private ApartmentRepository apartmentRepository;

    @Mock
    private ApartmentMapper apartmentMapper;

    @InjectMocks
    private ApartmentServiceImpl apartmentService;

    private Apartment apartment;
    private ApartmentDomain apartmentDomain;

    @BeforeEach
    void setUp() {
        apartment = Apartment.builder()
                .id(1L)
                .apartmentName("Prestige Falcon City")
                .address(new ApartmentAddress("Kanakapura Road", "", "Bengaluru", "Karnataka", "India", "560062"))
                .build();

        apartmentDomain = ApartmentDomain.builder()
                .id(1L)
                .apartmentName("Prestige Falcon City")
                .apartmentAddress(new ApartmentAddress("Kanakapura Road", "", "Bengaluru", "Karnataka", "India", "560062"))
                .build();
    }

    @Test
    void getApartmentById_whenExists_shouldReturnApartment() {
        // Arrange
        when(apartmentRepository.findById(1L)).thenReturn(Optional.of(apartment));
        when(apartmentMapper.toDomain(apartment)).thenReturn(apartmentDomain);

        // Act
        ApartmentDomain foundDomain = apartmentService.getApartmentById(1L);

        // Assert
        assertThat(foundDomain).isNotNull();
        assertThat(foundDomain.getId()).isEqualTo(1L);
        verify(apartmentRepository).findById(1L);
        verify(apartmentMapper).toDomain(apartment);
    }

    @Test
    void getApartmentById_whenNotExists_shouldThrowResourceNotFoundException() {
        // Arrange
        when(apartmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            apartmentService.getApartmentById(1L);
        });
        verify(apartmentRepository).findById(1L);
    }

    @Test
    void saveApartment_shouldCallRepositoryAndMapper() {
        // Arrange
        when(apartmentMapper.toEntity(any(ApartmentDomain.class))).thenReturn(apartment);
        when(apartmentRepository.save(any(Apartment.class))).thenReturn(apartment);
        when(apartmentMapper.toDomain(any(Apartment.class))).thenReturn(apartmentDomain);

        // Act
        ApartmentDomain savedDomain = apartmentService.saveApartment(ApartmentDomain.builder().build());

        // Assert
        assertThat(savedDomain).isNotNull();
        verify(apartmentMapper).toEntity(any(ApartmentDomain.class));
        verify(apartmentRepository).save(any(Apartment.class));
        verify(apartmentMapper).toDomain(any(Apartment.class));
    }

    @Test
    void updateApartment_whenExists_shouldUpdateAndReturnApartment() {
        // Arrange
        when(apartmentRepository.findById(1L)).thenReturn(Optional.of(apartment));
        when(apartmentRepository.save(any(Apartment.class))).thenReturn(apartment);
        when(apartmentMapper.toDomain(apartment)).thenReturn(apartmentDomain);

        // Act
        ApartmentDomain updatedDomain = apartmentService.updateApartment(1L, apartmentDomain);

        // Assert
        assertThat(updatedDomain).isNotNull();
        verify(apartmentRepository).findById(1L);
        verify(apartmentRepository).save(apartment);
        verify(apartmentMapper).updateEntity(apartmentDomain, apartment);
    }

    @Test
    void deleteApartment_whenExists_shouldCallRepositoryDelete() {
        // Arrange
        when(apartmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(apartmentRepository).deleteById(1L);

        // Act
        apartmentService.deleteApartment(1L);

        // Assert
        verify(apartmentRepository).existsById(1L);
        verify(apartmentRepository).deleteById(1L);
    }

    @Test
    void searchApartmentsByName_shouldReturnMatchingPagedApartments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Apartment> apartmentPage = new PageImpl<>(List.of(apartment), pageable, 1);
        when(apartmentRepository.findByApartmentNameContainingIgnoreCase(eq("Prestige"), any(Pageable.class)))
                .thenReturn(apartmentPage);
        when(apartmentMapper.toDomain(any(Apartment.class))).thenReturn(apartmentDomain);

        // Act
        Page<ApartmentDomain> results = apartmentService.searchApartmentsByName("Prestige", pageable);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getApartmentName()).isEqualTo("Prestige Falcon City");
        verify(apartmentRepository).findByApartmentNameContainingIgnoreCase(eq("Prestige"), any(Pageable.class));
        verify(apartmentMapper, times(1)).toDomain(any(Apartment.class));
    }

    @Test
    void getAllApartments_shouldReturnPagedApartments() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Apartment> apartmentPage = new PageImpl<>(List.of(apartment), pageable, 1);
        when(apartmentRepository.findAll(any(Pageable.class))).thenReturn(apartmentPage);
        when(apartmentMapper.toDomain(any(Apartment.class))).thenReturn(apartmentDomain);

        // Act
        Page<ApartmentDomain> results = apartmentService.getAllApartments(pageable);

        // Assert
        assertThat(results).isNotNull();
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getApartmentName()).isEqualTo("Prestige Falcon City");
        verify(apartmentRepository).findAll(any(Pageable.class));
        verify(apartmentMapper, times(1)).toDomain(any(Apartment.class));
    }
}
