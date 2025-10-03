package com.community.ecommerce.userservice.mapper;

import com.community.ecommerce.userservice.dto.UserRegistrationRequest;
import com.community.ecommerce.userservice.dto.UserUpdateRequest;
import com.community.ecommerce.userservice.dto.UserResponse;
import com.community.ecommerce.userservice.entity.Role;
import com.community.ecommerce.userservice.entity.User;
import com.community.ecommerce.userservice.model.RoleDomain;
import com.community.ecommerce.userservice.model.UserDomain;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // --- DTO to Domain ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserDomain toDomain(UserRegistrationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "apartmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserDomain toDomain(UserUpdateRequest request);

    // --- Entity to Domain ---
    @Mapping(source = "status", target = "status")
    UserDomain toDomain(User user);
    RoleDomain toDomain(Role role);

    // --- Domain to Entity ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", source = "role")
    User toEntity(UserDomain userDomain);
    Role toEntity(RoleDomain roleDomain);

    // --- Domain to Response ---
    @Mapping(source = "role.name", target = "roleName")
    UserResponse toResponse(UserDomain userDomain);

    // --- Update existing Entity from Domain ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "role", source = "role")
    void updateEntity(UserDomain userDomain, @MappingTarget User user);
}
