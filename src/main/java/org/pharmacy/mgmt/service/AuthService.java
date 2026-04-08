package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.security.JwtTokenProvider;
import java.util.Map;
import org.pharmacy.mgmt.dto.AuthResponse;
import org.pharmacy.mgmt.dto.LoginRequest;
import org.pharmacy.mgmt.dto.ResetPasswordRequest;
import org.pharmacy.mgmt.dto.SignupRequest;
import org.pharmacy.mgmt.dto.UpdateUserRequest;
import org.pharmacy.mgmt.dto.UserResponseDTO;
import org.pharmacy.mgmt.exception.InvalidCredentialsException;
import org.pharmacy.mgmt.exception.UserAlreadyExistsException;
import org.pharmacy.mgmt.model.Role;
import org.pharmacy.mgmt.model.User;
import org.pharmacy.mgmt.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .password_hash(passwordEncoder.encode(request.getPassword()))
            .full_name(request.getFull_name())
                .role(request.getRole() != null ? request.getRole() : Role.Pharmacist)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully")
                .username(user.getUsername())
                .role(user.getRole())
                .full_name(user.getFull_name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!isValidPassword(request.getPassword(), user.getPassword_hash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // generate token
        String token = jwtTokenProvider.generateToken(user.getUsername(), Map.of("role", user.getRole().name()));

        return AuthResponse.builder()
            .message("Login successful")
            .username(user.getUsername())
            .role(user.getRole())
            .full_name(user.getFull_name())
            .token(token)
            .build();
    }

    private boolean isValidPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        // System.out.println("Raw password: " + rawPassword + ", Stored password: " + storedPassword);
        // System.out.println("Encoded Password: " + passwordEncoder.encode(rawPassword));
        // System.out.println(passwordEncoder.matches(rawPassword, storedPassword));
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or full name"));

        if (!user.getFull_name().equalsIgnoreCase(request.getFull_name())) {
            throw new InvalidCredentialsException("Invalid username or full name");
        }

        user.setPassword_hash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return AuthResponse.builder()
                .message("Password reset successfully")
                .username(user.getUsername())
                .role(user.getRole())
                .full_name(user.getFull_name())
                .build();
    }

    public UserResponseDTO fetchUserByEmail(String email) {
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found for email: " + email));

        return toUserResponse(user);
    }

    public UserResponseDTO updateUser(String username, UpdateUserRequest payload) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found: " + username));

        if (payload.getUsername() != null && !payload.getUsername().isBlank()
                && !payload.getUsername().equalsIgnoreCase(username)) {
            if (userRepository.findByUsername(payload.getUsername()).isPresent()) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(payload.getUsername());
        }

        if (payload.getFull_name() != null && !payload.getFull_name().isBlank()) {
            user.setFull_name(payload.getFull_name());
        }

        if (payload.getRole() != null) {
            user.setRole(payload.getRole());
        }

        if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
            user.setPassword_hash(passwordEncoder.encode(payload.getPassword()));
        }

        User updated = userRepository.save(user);
        return toUserResponse(updated);
    }

    private UserResponseDTO toUserResponse(User user) {
        return UserResponseDTO.builder()
                .username(user.getUsername())
                .full_name(user.getFull_name())
                .role(user.getRole())
                .build();
    }
}
