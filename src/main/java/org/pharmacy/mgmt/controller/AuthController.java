package org.pharmacy.mgmt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.AuthResponse;
import org.pharmacy.mgmt.dto.LoginRequest;
import org.pharmacy.mgmt.dto.ResetPasswordRequest;
import org.pharmacy.mgmt.dto.SignupRequest;
import org.pharmacy.mgmt.dto.UpdateUserRequest;
import org.pharmacy.mgmt.dto.UserResponseDTO;
import org.pharmacy.mgmt.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/auth/reset")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/users/by-email")
    public ResponseEntity<UserResponseDTO> fetchUserByEmail(@RequestParam("email") String email) {
        System.out.println("Fetching user by email: " + email);
        return ResponseEntity.ok(authService.fetchUserByEmail(email));
    }

    @PutMapping("/users/{username}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable String username,
                                                      @Valid @RequestBody UpdateUserRequest payload) {
        return ResponseEntity.ok(authService.updateUser(username, payload));
    }
}
