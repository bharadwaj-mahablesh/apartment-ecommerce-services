package com.community.ecommerce.userservice.dto;

import com.community.ecommerce.common.events.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "User status cannot be null")
        UserStatus status,
        String roleName // Optional: if role changes upon status update
) {}
