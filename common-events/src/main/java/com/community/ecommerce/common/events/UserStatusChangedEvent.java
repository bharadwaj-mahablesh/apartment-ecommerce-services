package com.community.ecommerce.common.events;

import lombok.Builder;

public record UserStatusChangedEvent(
        Long userId,
        String email,
        String firstName,
        String lastName,
        UserStatus oldStatus,
        UserStatus newStatus,
        String oldRoleName,
        String newRoleName,
        Long apartmentId,
        String blockName,
        String apartmentNumber
) {}
