package org.pharmacy.mgmt.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.pharmacy.mgmt.repository.UserRepository;
import org.pharmacy.mgmt.security.JwtTokenProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/dev-token")
    public ResponseEntity<Map<String, String>> devToken(@RequestParam @NotBlank String username) {
        return userRepository.findByUsername(username)
                .map(u -> {
                    String token = jwtTokenProvider.generateToken(u.getUsername(), Map.of("role", u.getRole().name()));
                    return ResponseEntity.ok(Map.of("token", token));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "user not found")));
    }
}
