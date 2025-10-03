package com.community.ecommerce.common.events;

import lombok.Builder;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String firstName,
        String lastName,
        Long apartmentId,
        String blockName,
        String apartmentNumber,
        UserStatus status,
        String roleName
) {}
