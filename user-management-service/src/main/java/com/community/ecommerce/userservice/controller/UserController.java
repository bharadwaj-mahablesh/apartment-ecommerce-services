package com.community.ecommerce.userservice.controller;

import com.community.ecommerce.userservice.dto.*;
import com.community.ecommerce.userservice.mapper.UserMapper;
import com.community.ecommerce.userservice.model.UserDomain;
import com.community.ecommerce.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing user profiles and registration")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Register a new user",
               description = "Registers a new user with PENDING_APPROVAL status and PENDING_USER role.",
               responses = {
                   @ApiResponse(responseCode = "201", description = "User registered successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid input"),
                   @ApiResponse(responseCode = "404", description = "Apartment or Block not found"),
                   @ApiResponse(responseCode = "409", description = "User with email already exists")
               })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserDomain userDomain = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(userDomain));
    }

    @Operation(summary = "Get user by ID",
               description = "Retrieve a user's profile by their ID. ADMIN can view any user, RESIDENT can view their own.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "User found"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access"),
                   @ApiResponse(responseCode = "404", description = "User not found")
               })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserDomain userDomain = userService.getUserById(id);
        return ResponseEntity.ok(userMapper.toResponse(userDomain));
    }

    @Operation(summary = "Get all users",
               description = "Retrieve a paginated and sortable list of all users. Only ADMINs can access.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "List of users retrieved"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access")
               })
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserDomain> userDomainPage = userService.getAllUsers(pageable);
        return ResponseEntity.ok(userDomainPage.map(userMapper::toResponse));
    }

    @Operation(summary = "Update user profile",
               description = "Update an existing user's profile. ADMIN can update any user, RESIDENT can update their own.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "User updated successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid input"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access"),
                   @ApiResponse(responseCode = "404", description = "User not found")
               })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserDomain updatedUserDomain = userService.updateUser(id, request);
        return ResponseEntity.ok(userMapper.toResponse(updatedUserDomain));
    }

    @Operation(summary = "Delete a user",
               description = "Delete a user by ID. Only ADMINs can delete users.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access"),
                   @ApiResponse(responseCode = "404", description = "User not found")
               })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change user password",
               description = "Change the password for a user. ADMIN can change any user's password, RESIDENT can change their own.",
               responses = {
                   @ApiResponse(responseCode = "204", description = "Password changed successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid input or old password mismatch"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access"),
                   @ApiResponse(responseCode = "404", description = "User not found")
               })
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Approve or reject user registration",
               description = "Changes a user's status and optionally assigns a new role upon approval. Only ADMINs can access.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "User status updated successfully"),
                   @ApiResponse(responseCode = "400", description = "Invalid status transition or role not found"),
                   @ApiResponse(responseCode = "403", description = "Forbidden access"),
                   @ApiResponse(responseCode = "404", description = "User not found")
               })
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponse> approveOrRejectUser(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        UserDomain updatedUserDomain = userService.approveOrRejectUser(id, request);
        return ResponseEntity.ok(userMapper.toResponse(updatedUserDomain));
    }
}
