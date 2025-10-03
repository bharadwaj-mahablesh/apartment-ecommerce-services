package com.community.ecommerce.userservice.model;

import com.community.ecommerce.common.events.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDomain {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password; // This will be the hashed password
    private Long apartmentId;
    private String blockName;
    private String apartmentNumber;
    private UserStatus status;
    private RoleDomain role;
    private LocalDateTime createdAt;
}
