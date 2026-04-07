package org.pharmacy.mgmt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.dto.AuthResponse;
import org.pharmacy.mgmt.dto.LoginRequest;
import org.pharmacy.mgmt.dto.ResetPasswordRequest;
import org.pharmacy.mgmt.dto.SignupRequest;
import org.pharmacy.mgmt.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/reset")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
