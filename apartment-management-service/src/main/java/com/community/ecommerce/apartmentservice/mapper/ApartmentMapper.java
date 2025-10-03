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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper
public interface ApartmentMapper {

    // --- DTO to Domain ---
    @Mapping(source = "apartmentPointOfContacts", target = "apartmentContactsDomainList")
    @Mapping(source = "blockNames", target = "apartmentBlocksDomainList", qualifiedByName = "stringToApartmentBlocksDomain")
    ApartmentDomain toDomain(ApartmentRegistrationRequest request);

    // --- Entity to Domain ---
    @Mapping(source = "address", target = "apartmentAddress")
    @Mapping(source = "apartmentContacts", target = "apartmentContactsDomainList")
    @Mapping(source = "apartmentBlocks", target = "apartmentBlocksDomainList")
    ApartmentDomain toDomain(Apartment apartment);

    // --- Domain to Entity ---
    @Mapping(source = "apartmentAddress", target = "address")
    @Mapping(source = "apartmentContactsDomainList", target = "apartmentContacts")
    @Mapping(source = "apartmentBlocksDomainList", target = "apartmentBlocks")
    @Mapping(target = "createdBy", ignore = true)
    Apartment toEntity(ApartmentDomain domain);

    // --- Domain to Response ---
    @Mapping(source = "apartmentAddress", target = "address")
    @Mapping(source = "apartmentContactsDomainList", target = "contacts")
    @Mapping(source = "apartmentBlocksDomainList", target = "blockNames", qualifiedByName = "apartmentBlocksDomainToString")
    ApartmentResponse toResponse(ApartmentDomain domain);

    List<ApartmentResponse> toResponseList(List<ApartmentDomain> domainList);

    // --- Update existing Entity from Domain ---
    @Mapping(source = "apartmentAddress", target = "address")
    @Mapping(source = "apartmentContactsDomainList", target = "apartmentContacts")
    @Mapping(source = "apartmentBlocksDomainList", target = "apartmentBlocks")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ApartmentDomain domain, @MappingTarget Apartment apartment);

    // --- Helper methods for nested objects ---

    // Address
    @Mapping(source = "cityName", target = "city")
    @Mapping(source = "stateName", target = "state")
    @Mapping(source = "countryName", target = "country")
    @Mapping(source = "zipCode", target = "zipcode")
    ApartmentAddress toAddress(ApartmentAddressDTO dto);

    @Mapping(source = "city", target = "cityName")
    @Mapping(source = "state", target = "stateName")
    @Mapping(source = "country", target = "countryName")
    @Mapping(source = "zipcode", target = "zipCode")
    ApartmentAddressDTO toAddressDTO(ApartmentAddress address);

    // Contacts
    List<ApartmentContactsDomain> toContactsDomainList(List<ApartmentContactDTO> dtos);

    @Mapping(source = "name", target = "contactName")
    @Mapping(source = "emailAddress", target = "contactEmail")
    @Mapping(source = "phoneNumber", target = "contactPhoneNumber")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ApartmentContactsDomain toContactDomain(ApartmentContactDTO dto);

    @Mapping(source = "contactName", target = "name")
    @Mapping(source = "contactEmail", target = "emailAddress")
    @Mapping(source = "contactPhoneNumber", target = "phoneNumber")
    ApartmentContactDTO toContactDTO(ApartmentContactsDomain domain);

    List<ApartmentContacts> toContactsEntityList(List<ApartmentContactsDomain> domains);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apartment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    ApartmentContacts toContactEntity(ApartmentContactsDomain domain);

    // Blocks
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apartment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    ApartmentBlocks toBlockEntity(ApartmentBlocksDomain domain);

    @Named("stringToApartmentBlocksDomain")
    default List<ApartmentBlocksDomain> stringToApartmentBlocksDomain(List<String> blockNames) {
        if (blockNames == null) return java.util.Collections.emptyList();
        return blockNames.stream()
                .map(name -> ApartmentBlocksDomain.builder().blockName(name).build())
                .toList();
    }

    @Named("apartmentBlocksDomainToString")
    default List<String> apartmentBlocksDomainToString(List<ApartmentBlocksDomain> blocks) {
        if (blocks == null) return java.util.Collections.emptyList();
        return blocks.stream()
                .map(ApartmentBlocksDomain::getBlockName)
                .toList();
    }
}
