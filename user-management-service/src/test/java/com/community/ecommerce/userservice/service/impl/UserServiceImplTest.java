package com.community.ecommerce.userservice.service.impl;

import com.community.ecommerce.common.events.UserStatus;
import com.community.ecommerce.userservice.dto.ApartmentDetailsResponse;
import com.community.ecommerce.userservice.dto.UserRegistrationRequest;
import com.community.ecommerce.userservice.dto.UserStatusUpdateRequest;
import com.community.ecommerce.userservice.dto.UserUpdateRequest;
import com.community.ecommerce.userservice.entity.Role;
import com.community.ecommerce.userservice.entity.User;
import com.community.ecommerce.userservice.exception.ResourceNotFoundException;
import com.community.ecommerce.userservice.mapper.UserMapper;
import com.community.ecommerce.userservice.model.RoleDomain;
import com.community.ecommerce.userservice.model.UserDomain;
import com.community.ecommerce.userservice.repository.RoleRepository;
import com.community.ecommerce.userservice.repository.UserRepository;
import com.community.ecommerce.userservice.service.UserEventProducer;
import com.community.ecommerce.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationRequest registrationRequest;
    private User userEntity;
    private UserDomain userDomain;
    private Role defaultRoleEntity;
    private RoleDomain defaultRoleDomain;

    @BeforeEach
    void setUp() {
        registrationRequest = new UserRegistrationRequest(
                "John", "Doe", "john.doe@example.com", "password123", 1L, "Block A", "101");

        defaultRoleEntity = Role.builder().id(1L).name("PENDING_USER").build();
        defaultRoleDomain = RoleDomain.builder().id(1L).name("PENDING_USER").build();

        userEntity = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("hashedPassword")
                .apartmentId(1L)
                .blockName("Block A")
                .apartmentNumber("101")
                .status(UserStatus.PENDING_APPROVAL)
                .role(defaultRoleEntity)
                .build();

        userDomain = UserDomain.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("hashedPassword")
                .apartmentId(1L)
                .blockName("Block A")
                .apartmentNumber("101")
                .status(UserStatus.PENDING_APPROVAL)
                .role(defaultRoleDomain)
                .build();
    }

    @Test
    void registerUser_success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toDomain(any(UserRegistrationRequest.class))).thenReturn(userDomain);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName(eq("PENDING_USER"))).thenReturn(Optional.of(defaultRoleEntity));
        when(userMapper.toDomain(any(Role.class))).thenReturn(defaultRoleDomain);
        when(userMapper.toEntity(any(UserDomain.class))).thenReturn(userEntity);
        when(userRepository.save(any(User.class))).thenReturn(userEntity);
        when(userMapper.toDomain(any(User.class))).thenReturn(userDomain);

        ApartmentDetailsResponse apartmentDetails = new ApartmentDetailsResponse(1L, List.of("Block A", "Block B"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(apartmentDetails);

        // Act
        UserDomain result = userService.registerUser(registrationRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING_APPROVAL);
        assertThat(result.getRole().getName()).isEqualTo("PENDING_USER");
        verify(userRepository).existsByEmail(registrationRequest.email());
        verify(restTemplate).getForObject(anyString(), eq(ApartmentDetailsResponse.class));
        verify(passwordEncoder).encode(registrationRequest.password());
        verify(roleRepository).findByName("PENDING_USER");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDomain(any(UserRegistrationRequest.class));
        verify(userMapper).toDomain(any(Role.class));
        verify(userMapper).toEntity(any(UserDomain.class));
        verify(userMapper).toDomain(any(User.class));
    }

    @Test
    void registerUser_emailAlreadyExists_throwsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registrationRequest);
        });
        verify(userRepository).existsByEmail(registrationRequest.email());
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void registerUser_apartmentNotFound_throwsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.registerUser(registrationRequest);
        });
        verify(userRepository).existsByEmail(registrationRequest.email());
        verify(restTemplate).getForObject(anyString(), eq(ApartmentDetailsResponse.class));
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void registerUser_blockNotFound_throwsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        ApartmentDetailsResponse apartmentDetails = new ApartmentDetailsResponse(1L, List.of("Block B", "Block C"));
        when(restTemplate.getForObject(anyString(), eq(ApartmentDetailsResponse.class)))
                .thenReturn(apartmentDetails);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.registerUser(registrationRequest);
        });
        verify(userRepository).existsByEmail(registrationRequest.email());
        verify(restTemplate).getForObject(anyString(), eq(ApartmentDetailsResponse.class));
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void getUserById_success() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(userMapper.toDomain(any(User.class))).thenReturn(userDomain);

        // Act
        UserDomain result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
        verify(userMapper).toDomain(userEntity);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void getUserById_notFound_throwsException() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(1L);
        });
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void getAllUsers_success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(userEntity), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDomain(any(User.class))).thenReturn(userDomain);

        // Act
        Page<UserDomain> result = userService.getAllUsers(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findAll(pageable);
        verify(userMapper, times(1)).toDomain(userEntity);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void updateUser_success() {
        // Arrange
        UserUpdateRequest updateRequest = new UserUpdateRequest("Jane", "Doe", "jane.doe@example.com", "Block B", "202");
        User updatedUserEntity = User.builder()
                .id(1L).firstName("Jane").lastName("Doe").email("jane.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block B").apartmentNumber("202")
                .status(UserStatus.PENDING_APPROVAL).role(defaultRoleEntity).build();
        UserDomain updatedUserDomain = UserDomain.builder()
                .id(1L).firstName("Jane").lastName("Doe").email("jane.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block B").apartmentNumber("202")
                .status(UserStatus.PENDING_APPROVAL).role(defaultRoleDomain).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(User.class))).thenReturn(updatedUserEntity);
        when(userMapper.toDomain(any(User.class))).thenReturn(updatedUserDomain);

        // Act
        UserDomain result = userService.updateUser(1L, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getEmail()).isEqualTo("jane.doe@example.com");
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDomain(any(User.class));
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void updateUser_notFound_throwsException() {
        // Arrange
        UserUpdateRequest updateRequest = new UserUpdateRequest("Jane", "Doe", "jane.doe@example.com", "Block B", "202");
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(1L, updateRequest);
        });
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void deleteUser_success() {
        // Arrange
        when(userRepository.existsById(anyLong())).thenReturn(true);
        doNothing().when(userRepository).deleteById(anyLong());

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void deleteUser_notFound_throwsException() {
        // Arrange
        when(userRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(1L);
        });
        verify(userRepository).existsById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void changePassword_success() {
        // Arrange
        String originalHashedPassword = userEntity.getPassword(); // Capture original password
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches(eq("password123"), eq(originalHashedPassword))).thenReturn(true);
        when(passwordEncoder.encode(eq("newPassword123"))).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(userEntity);

        // Act
        userService.changePassword(1L, "password123", "newPassword123");

        // Assert
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("password123", originalHashedPassword);
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(userEntity);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void changePassword_notFound_throwsException() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.changePassword(1L, "oldPass", "newPass");
        });
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void changePassword_oldPasswordMismatch_throwsException() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches(eq("wrongOldPass"), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(1L, "wrongOldPass", "newPass");
        });
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrongOldPass", userEntity.getPassword());
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void approveOrRejectUser_approveSuccess() {
        // Arrange
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.APPROVED, "RESIDENT");
        User userToApprove = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.PENDING_APPROVAL).role(defaultRoleEntity).build();
        User approvedUserEntity = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.APPROVED).role(Role.builder().id(2L).name("RESIDENT").build()).build();
        UserDomain approvedUserDomain = UserDomain.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.APPROVED).role(RoleDomain.builder().id(2L).name("RESIDENT").build()).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userToApprove));
        when(roleRepository.findByName(eq("RESIDENT"))).thenReturn(Optional.of(Role.builder().id(2L).name("RESIDENT").build()));
        when(userRepository.save(any(User.class))).thenReturn(approvedUserEntity);
        when(userMapper.toDomain(any(User.class))).thenReturn(approvedUserDomain);

        // Act
        UserDomain result = userService.approveOrRejectUser(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.APPROVED);
        assertThat(result.getRole().getName()).isEqualTo("RESIDENT");
        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("RESIDENT");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDomain(any(User.class));
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void approveOrRejectUser_rejectSuccess() {
        // Arrange
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.REJECTED, null);
        User userToReject = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.PENDING_APPROVAL).role(defaultRoleEntity).build();
        User rejectedUserEntity = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.REJECTED).role(defaultRoleEntity).build();
        UserDomain rejectedUserDomain = UserDomain.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.REJECTED).role(defaultRoleDomain).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userToReject));
        when(userRepository.save(any(User.class))).thenReturn(rejectedUserEntity);
        when(userMapper.toDomain(any(User.class))).thenReturn(rejectedUserDomain);

        // Act
        UserDomain result = userService.approveOrRejectUser(1L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(result.getRole().getName()).isEqualTo("PENDING_USER"); // Role remains PENDING_USER
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDomain(any(User.class));
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void approveOrRejectUser_userNotFound_throwsException() {
        // Arrange
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.APPROVED, "RESIDENT");
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.approveOrRejectUser(1L, request);
        });
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void approveOrRejectUser_roleNotFound_throwsException() {
        // Arrange
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.APPROVED, "NON_EXISTENT_ROLE");
        User userToApprove = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.PENDING_APPROVAL).role(defaultRoleEntity).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userToApprove));
        when(roleRepository.findByName(eq("NON_EXISTENT_ROLE"))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.approveOrRejectUser(1L, request);
        });
        verify(userRepository).findById(1L);
        verify(roleRepository).findByName("NON_EXISTENT_ROLE");
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }

    @Test
    void approveOrRejectUser_invalidStatusTransition_throwsException() {
        // Arrange
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(UserStatus.PENDING_APPROVAL, null);
        User userAlreadyApproved = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .password("hashedPassword").apartmentId(1L).blockName("Block A").apartmentNumber("101")
                .status(UserStatus.APPROVED).role(defaultRoleEntity).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(userAlreadyApproved));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.approveOrRejectUser(1L, request);
        });
        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository, restTemplate, passwordEncoder, roleRepository, userMapper);
    }
}
