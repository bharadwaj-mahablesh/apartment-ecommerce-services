package com.community.ecommerce.userservice.dto;

import com.community.ecommerce.common.events.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Long apartmentId,
        String blockName,
        String apartmentNumber,
        UserStatus status,
        String roleName,
        LocalDateTime createdAt
) {}
