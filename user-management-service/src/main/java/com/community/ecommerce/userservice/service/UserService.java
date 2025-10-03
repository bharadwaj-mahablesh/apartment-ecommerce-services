package com.community.ecommerce.userservice.service;

import com.community.ecommerce.userservice.dto.UserRegistrationRequest;
import com.community.ecommerce.userservice.dto.UserStatusUpdateRequest;
import com.community.ecommerce.userservice.dto.UserUpdateRequest;
import com.community.ecommerce.userservice.model.UserDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDomain registerUser(UserRegistrationRequest request);
    UserDomain getUserById(Long id);
    Page<UserDomain> getAllUsers(Pageable pageable);
    UserDomain updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    void changePassword(Long id, String oldPassword, String newPassword);
    UserDomain approveOrRejectUser(Long userId, UserStatusUpdateRequest request);
}
