package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.LoginRequest;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.exception.BusinessException;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.repository.UserRepository;
import com.microtech.smartshop.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        hashedPassword = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"; // Hash de "password"

        testUser = User.builder()
                .id("user-1")
                .username("testuser")
                .motDePasse(hashedPassword)
                .role(UserRole.CLIENT)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
    }

    @Test
    void testAuthenticate_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("password"))
                    .thenReturn(hashedPassword);

            User result = authService.authenticate(loginRequest);

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals(UserRole.CLIENT, result.getRole());
            verify(userRepository, times(1)).findByUsername("testuser");
        }
    }

    @Test
    void testAuthenticate_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.authenticate(loginRequest);
        });

        assertEquals("Utilisateur introuvable", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testAuthenticate_IncorrectPassword_ThrowsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("wrongpassword"))
                    .thenReturn("wronghash123");

            loginRequest.setPassword("wrongpassword");

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.authenticate(loginRequest);
            });

            assertEquals("Mot de passe incorrect", exception.getMessage());
            verify(userRepository, times(1)).findByUsername("testuser");
        }
    }

    @Test
    void testAuthenticate_NullUsername_ThrowsException() {
        loginRequest.setUsername(null);

        assertThrows(Exception.class, () -> {
            authService.authenticate(loginRequest);
        });
    }

    @Test
    void testAuthenticate_EmptyPassword() {
        loginRequest.setPassword("");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(""))
                    .thenReturn("emptyhash");

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                authService.authenticate(loginRequest);
            });

            assertEquals("Mot de passe incorrect", exception.getMessage());
        }
    }

    @Test
    void testAuthenticate_AdminUser_Success() {
        User adminUser = User.builder()
                .id("admin-1")
                .username("admin")
                .motDePasse(hashedPassword)
                .role(UserRole.ADMIN)
                .build();

        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("password"))
                    .thenReturn(hashedPassword);

            User result = authService.authenticate(loginRequest);

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
            assertEquals(UserRole.ADMIN, result.getRole());
        }
    }

    @Test
    void testAuthenticate_CaseSensitiveUsername() {
        loginRequest.setUsername("TestUser"); // Différence de casse
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            authService.authenticate(loginRequest);
        });
    }

    @Test
    void testAuthenticate_PasswordHashing() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtil> mockedPasswordUtil = mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword("password"))
                    .thenReturn(hashedPassword);

            User result = authService.authenticate(loginRequest);

            assertNotNull(result);
            mockedPasswordUtil.verify(() -> PasswordUtil.hashPassword("password"), times(1));
        }
    }
}

