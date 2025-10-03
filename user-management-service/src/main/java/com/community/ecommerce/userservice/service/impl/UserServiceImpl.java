package com.community.ecommerce.userservice.service.impl;

import com.community.ecommerce.userservice.dto.ApartmentDetailsResponse;
import com.community.ecommerce.userservice.dto.UserRegistrationRequest;
import com.community.ecommerce.userservice.dto.UserStatusUpdateRequest;
import com.community.ecommerce.userservice.dto.UserUpdateRequest;
import com.community.ecommerce.userservice.entity.Role;
import com.community.ecommerce.userservice.entity.User;
import com.community.ecommerce.common.events.UserRegisteredEvent;
import com.community.ecommerce.common.events.UserStatusChangedEvent;
import com.community.ecommerce.common.events.UserStatus;
import com.community.ecommerce.userservice.exception.ResourceNotFoundException;
import com.community.ecommerce.userservice.mapper.UserMapper;
import com.community.ecommerce.userservice.model.UserDomain;
import com.community.ecommerce.userservice.repository.RoleRepository;
import com.community.ecommerce.userservice.repository.UserRepository;
import com.community.ecommerce.userservice.service.UserEventProducer;
import com.community.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final UserEventProducer userEventProducer;

    @Value("${apartment.service.url}")
    private String apartmentServiceUrl;

    @Override
    public UserDomain registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Verify apartment existence via inter-service communication
        String apartmentVerificationUrl = apartmentServiceUrl + "/api/v1/apartments/" + request.apartmentId();
        ApartmentDetailsResponse apartmentDetails;
        try {
            apartmentDetails = restTemplate.getForObject(apartmentVerificationUrl, ApartmentDetailsResponse.class);
            if (apartmentDetails == null || apartmentDetails.blockNames() == null || !apartmentDetails.blockNames().contains(request.blockName())) {
                throw new ResourceNotFoundException("Block '" + request.blockName() + "' not found in Apartment with ID " + request.apartmentId() + ".");
            }
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Apartment with ID " + request.apartmentId() + " not found. Your apartment is not onboarded yet.");
            } else {
                throw new RuntimeException("Error verifying apartment: " + ex.getMessage());
            }
        }

        UserDomain userDomain = userMapper.toDomain(request);
        userDomain.setPassword(passwordEncoder.encode(request.password()));
        userDomain.setStatus(UserStatus.PENDING_APPROVAL);

        // Assign default role (PENDING_USER)
        Role pendingUserRole = roleRepository.findByName("PENDING_USER")
                .orElseThrow(() -> new IllegalStateException("PENDING_USER role not found. Please pre-populate roles."));
        userDomain.setRole(userMapper.toDomain(pendingUserRole));

        User userEntity = userMapper.toEntity(userDomain);
        User savedUserEntity = userRepository.save(userEntity);

        // Publish UserRegisteredEvent
        userEventProducer.publishUserRegisteredEvent(new UserRegisteredEvent(
                savedUserEntity.getId(),
                savedUserEntity.getEmail(),
                savedUserEntity.getFirstName(),
                savedUserEntity.getLastName(),
                savedUserEntity.getApartmentId(),
                savedUserEntity.getBlockName(),
                savedUserEntity.getApartmentNumber(),
                savedUserEntity.getStatus(),
                savedUserEntity.getRole().getName()
        ));

        return userMapper.toDomain(savedUserEntity);
    }

    @Override
    public UserDomain getUserById(Long id) {
        User userEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return userMapper.toDomain(userEntity);
    }

    @Override
    public Page<UserDomain> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public UserDomain updateUser(Long id, UserUpdateRequest request) {
        User existingUserEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Update fields from request
        existingUserEntity.setFirstName(request.firstName());
        existingUserEntity.setLastName(request.lastName());
        existingUserEntity.setEmail(request.email());
        existingUserEntity.setBlockName(request.blockName());
        existingUserEntity.setApartmentNumber(request.apartmentNumber());

        // Note: Password, apartmentId, status, role are not updated via this method.
        // Password changes will be a separate flow.
        // ApartmentId, status, role changes will be via admin/approval flows.

        User updatedUserEntity = userRepository.save(existingUserEntity);
        return userMapper.toDomain(updatedUserEntity);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User userEntity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (!passwordEncoder.matches(oldPassword, userEntity.getPassword())) {
            throw new IllegalArgumentException("Old password does not match.");
        }

        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
    }

    @Override
    public UserDomain approveOrRejectUser(Long userId, UserStatusUpdateRequest request) {
        User userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Capture old status and role for event
        UserStatus oldStatus = userEntity.getStatus();
        String oldRoleName = userEntity.getRole().getName();

        // Validate status transition (optional, but good practice)
        if (userEntity.getStatus() == UserStatus.APPROVED && request.status() == UserStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Cannot change status from APPROVED to PENDING_APPROVAL.");
        }

        userEntity.setStatus(request.status());

        // If approved, assign the specified role (e.g., RESIDENT)
        if (request.status() == UserStatus.APPROVED && request.roleName() != null) {
            Role newRole = roleRepository.findByName(request.roleName())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleName()));
            userEntity.setRole(newRole);
        } else if (request.status() == UserStatus.REJECTED) {
            // Optionally, assign a 'REJECTED' role or clear the role
            // For now, we'll keep the PENDING_USER role or whatever it was.
        }

        User updatedUserEntity = userRepository.save(userEntity);

        // Publish UserStatusChangedEvent
        userEventProducer.publishUserStatusChangedEvent(new UserStatusChangedEvent(
                updatedUserEntity.getId(),
                updatedUserEntity.getEmail(),
                updatedUserEntity.getFirstName(),
                updatedUserEntity.getLastName(),
                oldStatus,
                updatedUserEntity.getStatus(),
                oldRoleName,
                updatedUserEntity.getRole().getName(),
                updatedUserEntity.getApartmentId(),
                updatedUserEntity.getBlockName(),
                updatedUserEntity.getApartmentNumber()
        ));

        return userMapper.toDomain(updatedUserEntity);
    }
}
