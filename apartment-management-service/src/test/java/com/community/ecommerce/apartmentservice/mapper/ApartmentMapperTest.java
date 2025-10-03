package com.community.ecommerce.apartmentservice.mapper;

import com.community.ecommerce.apartmentservice.dto.ApartmentAddressDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentContactDTO;
import com.community.ecommerce.apartmentservice.dto.ApartmentRegistrationRequest;
import com.community.ecommerce.apartmentservice.dto.ApartmentResponse;
import com.community.ecommerce.apartmentservice.entity.Apartment;
import com.community.ecommerce.apartmentservice.entity.ApartmentAddress;
import com.community.ecommerce.apartmentservice.entity.ApartmentBlocks;
import com.community.ecommerce.apartmentservice.entity.ApartmentContacts;
import com.community.ecommerce.apartmentservice.model.ApartmentBlocksDomain;
import com.community.ecommerce.apartmentservice.model.ApartmentContactsDomain;
import com.community.ecommerce.apartmentservice.model.ApartmentDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ApartmentMapperTest {

    private ApartmentMapper apartmentMapper;

    @BeforeEach
    void setUp() {
        apartmentMapper = Mappers.getMapper(ApartmentMapper.class);
    }

    @Test
    void givenRequestDTO_whenMapToDomain_thenCorrectlyMapped() {
        // Arrange
        ApartmentRegistrationRequest request = new ApartmentRegistrationRequest(
                "Prestige Falcon City",
                new ApartmentAddressDTO("Kanakapura Road", "", "Bengaluru", "Karnataka", "India", "560062"),
                List.of(new ApartmentContactDTO("Priya Sharma", "priya.sharma@example.com", "+91 9876543210")),
                List.of("Tower A", "Tower B")
        );

        // Act
        ApartmentDomain domain = apartmentMapper.toDomain(request);

        // Assert
        assertThat(domain).isNotNull();
        assertThat(domain.getApartmentName()).isEqualTo("Prestige Falcon City");
        assertThat(domain.getApartmentAddress().getAddressLine1()).isEqualTo("Kanakapura Road");
        assertThat(domain.getApartmentContactsDomainList()).hasSize(1);
        assertThat(domain.getApartmentContactsDomainList().get(0).getContactName()).isEqualTo("Priya Sharma");
        assertThat(domain.getApartmentBlocksDomainList()).hasSize(2);
        assertThat(domain.getApartmentBlocksDomainList().get(0).getBlockName()).isEqualTo("Tower A");
    }

    @Test
    void givenEntity_whenMapToDomain_thenCorrectlyMapped() {
        // Arrange
        Apartment apartment = Apartment.builder()
                .id(1L)
                .apartmentName("Sobha Dream Gardens")
                .address(new ApartmentAddress("Thanisandra Main Rd", null, "Bengaluru", "Karnataka", "India", "560077"))
                .apartmentContacts(List.of(ApartmentContacts.builder().contactName("Arjun Kumar").build()))
                .apartmentBlocks(List.of(ApartmentBlocks.builder().blockName("Tower C").build()))
                .build();

        // Act
        ApartmentDomain domain = apartmentMapper.toDomain(apartment);

        // Assert
        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getApartmentName()).isEqualTo("Sobha Dream Gardens");
        assertThat(domain.getApartmentAddress().getCity()).isEqualTo("Bengaluru");
        assertThat(domain.getApartmentContactsDomainList()).hasSize(1);
        assertThat(domain.getApartmentContactsDomainList().get(0).getContactName()).isEqualTo("Arjun Kumar");
        assertThat(domain.getApartmentBlocksDomainList()).hasSize(1);
        assertThat(domain.getApartmentBlocksDomainList().get(0).getBlockName()).isEqualTo("Tower C");
    }

    @Test
    void givenDomain_whenMapToEntity_thenCorrectlyMapped() {
        // Arrange
        ApartmentDomain domain = ApartmentDomain.builder()
                .apartmentName("Brigade Gateway")
                .apartmentAddress(new ApartmentAddress("Dr Rajkumar Road", "Malleswaram", "Bengaluru", "Karnataka", "India", "560055"))
                .apartmentContactsDomainList(List.of(ApartmentContactsDomain.builder().contactName("Sunita Rao").build()))
                .apartmentBlocksDomainList(List.of(ApartmentBlocksDomain.builder().blockName("Orion").build()))
                .build();

        // Act
        Apartment entity = apartmentMapper.toEntity(domain);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getApartmentName()).isEqualTo("Brigade Gateway");
        assertThat(entity.getAddress().getCity()).isEqualTo("Bengaluru");
        assertThat(entity.getApartmentContacts()).hasSize(1);
        assertThat(entity.getApartmentContacts().get(0).getContactName()).isEqualTo("Sunita Rao");
        assertThat(entity.getApartmentBlocks()).hasSize(1);
        assertThat(entity.getApartmentBlocks().get(0).getBlockName()).isEqualTo("Orion");
    }

    @Test
    void givenDomain_whenMapToResponseDTO_thenCorrectlyMapped() {
        // Arrange
        ApartmentDomain domain = ApartmentDomain.builder()
                .id(1L)
                .apartmentName("Phoenix One Bangalore West")
                .apartmentAddress(new ApartmentAddress("1 Dr Rajkumar Rd", "Rajajinagar", "Bengaluru", "Karnataka", "India", "560010"))
                .apartmentContactsDomainList(List.of(ApartmentContactsDomain.builder().contactName("Vikram Singh").build()))
                .apartmentBlocksDomainList(List.of(ApartmentBlocksDomain.builder().blockName("Tower 5").build()))
                .build();

        // Act
        ApartmentResponse response = apartmentMapper.toResponse(domain);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.apartmentName()).isEqualTo("Phoenix One Bangalore West");
        assertThat(response.address().cityName()).isEqualTo("Bengaluru");
        assertThat(response.contacts()).hasSize(1);
        assertThat(response.contacts().get(0).name()).isEqualTo("Vikram Singh");
        assertThat(response.blockNames()).hasSize(1);
        assertThat(response.blockNames().get(0)).isEqualTo("Tower 5");
    }
}
