package org.pharmacy.mgmt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pharmacy.mgmt.dto.AuthResponse;
import org.pharmacy.mgmt.dto.LoginRequest;
import org.pharmacy.mgmt.dto.ResetPasswordRequest;
import org.pharmacy.mgmt.dto.SignupRequest;
import org.pharmacy.mgmt.exception.InvalidCredentialsException;
import org.pharmacy.mgmt.exception.UserAlreadyExistsException;
import org.pharmacy.mgmt.model.Role;
import org.pharmacy.mgmt.model.User;
import org.pharmacy.mgmt.repository.UserRepository;
import org.pharmacy.mgmt.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setPassword("password123");
        signupRequest.setFull_name("Test User");
        signupRequest.setRole(Role.Pharmacist);

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        user = User.builder()
                .user_id(1)
                .username("testuser")
                .password_hash("hashedPassword")
                .full_name("Test User")
                .role(Role.Pharmacist)
                .build();
    }

    @Test
    void signup_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.signup(signupRequest);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals("testuser", response.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void signup_UserAlreadyExists() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> authService.signup(signupRequest));
    }

    @Test
    void login_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyMap())).thenReturn("testToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void login_InvalidUsername() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_InvalidPassword() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setUsername("testuser");
        resetRequest.setFull_name("Test User");
        resetRequest.setNewPassword("newPassword123");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        AuthResponse response = authService.resetPassword(resetRequest);

        assertNotNull(response);
        assertEquals("Password reset successfully", response.getMessage());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void resetPassword_InvalidFullName() {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setUsername("testuser");
        resetRequest.setFull_name("Wrong Name");
        resetRequest.setNewPassword("newPassword123");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPassword(resetRequest));
    }
}
