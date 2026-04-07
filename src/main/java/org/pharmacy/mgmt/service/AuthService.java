package org.pharmacy.mgmt.service;

import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.AuthResponse;
import org.pharmacy.mgmt.dto.LoginRequest;
import org.pharmacy.mgmt.dto.ResetPasswordRequest;
import org.pharmacy.mgmt.dto.SignupRequest;
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

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword_hash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return AuthResponse.builder()
                .message("Login successful")
                .username(user.getUsername())
                .role(user.getRole())
                .full_name(user.getFull_name())
                .build();
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
}
