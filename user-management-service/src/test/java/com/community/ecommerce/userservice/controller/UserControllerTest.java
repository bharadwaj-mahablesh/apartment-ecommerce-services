package com.community.ecommerce.userservice.controller;

import com.community.ecommerce.common.events.UserStatus;
import com.community.ecommerce.userservice.dto.PasswordChangeRequest;
import com.community.ecommerce.userservice.dto.UserRegistrationRequest;
import com.community.ecommerce.userservice.dto.UserResponse;
import com.community.ecommerce.userservice.dto.UserStatusUpdateRequest;
import com.community.ecommerce.userservice.dto.UserUpdateRequest;
import com.community.ecommerce.userservice.mapper.UserMapper;
import com.community.ecommerce.userservice.model.UserDomain;
import com.community.ecommerce.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UserRegistrationRequest registrationRequest;
    private UserUpdateRequest updateRequest;
    private PasswordChangeRequest passwordChangeRequest;
    private UserStatusUpdateRequest statusUpdateRequest;
    private UserDomain userDomain;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        registrationRequest = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "password123", 1L, "Block A", "101");

        updateRequest = new UserUpdateRequest(
                "Jane", "Doe", "jane.doe@example.com", "Block B", "202");

        passwordChangeRequest = new PasswordChangeRequest("oldPassword", "newPassword");

        statusUpdateRequest = new UserStatusUpdateRequest(UserStatus.APPROVED, "RESIDENT");

        userDomain = UserDomain.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .apartmentId(1L).blockName("Block A").apartmentNumber("101").build();

        userResponse = new UserResponse(
                1L, "John", "Doe", "john.doe@example.com", 1L, "Block A", "101", null, "PENDING_USER", null);
    }

    @Test
    void registerUser_returnsCreated() throws Exception {
        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(userDomain);
        when(userMapper.toResponse(any(UserDomain.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(userService).registerUser(any(UserRegistrationRequest.class));
        verify(userMapper).toResponse(any(UserDomain.class));
    }

    @Test
    void getUserById_returnsOk() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(userDomain);
        when(userMapper.toResponse(any(UserDomain.class))).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).getUserById(anyLong());
        verify(userMapper).toResponse(any(UserDomain.class));
    }

    @Test
    void getAllUsers_returnsOk() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(userDomain), pageable, 1));
        when(userMapper.toResponse(any(UserDomain.class))).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("john.doe@example.com"));

        verify(userService).getAllUsers(any(Pageable.class));
        verify(userMapper, times(1)).toResponse(any(UserDomain.class));
    }

    @Test
    void updateUser_returnsOk() throws Exception {
        when(userService.updateUser(anyLong(), any(UserUpdateRequest.class))).thenReturn(userDomain);
        when(userMapper.toResponse(any(UserDomain.class))).thenReturn(userResponse);

        mockMvc.perform(put("/api/v1/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(userService).updateUser(anyLong(), any(UserUpdateRequest.class));
        verify(userMapper).toResponse(any(UserDomain.class));
    }

    @Test
    void deleteUser_returnsNoContent() throws Exception {
        doNothing().when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/api/v1/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(anyLong());
    }

    @Test
    void changePassword_returnsNoContent() throws Exception {
        doNothing().when(userService).changePassword(anyLong(), anyString(), anyString());

        mockMvc.perform(put("/api/v1/users/{id}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(anyLong(), anyString(), anyString());
    }

    @Test
    void approveOrRejectUser_returnsOk() throws Exception {
        UserDomain approvedUserDomain = UserDomain.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.APPROVED).build(); // Set status to APPROVED

        UserResponse approvedUserResponse = new UserResponse(
                1L, "John", "Doe", "john.doe@example.com", 1L, "Block A", "101", UserStatus.APPROVED, "RESIDENT", null);

        when(userService.approveOrRejectUser(anyLong(), any(UserStatusUpdateRequest.class))).thenReturn(approvedUserDomain);
        when(userMapper.toResponse(any(UserDomain.class))).thenReturn(approvedUserResponse);

        mockMvc.perform(put("/api/v1/users/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(userService).approveOrRejectUser(anyLong(), any(UserStatusUpdateRequest.class));
        verify(userMapper).toResponse(any(UserDomain.class));
    }
}