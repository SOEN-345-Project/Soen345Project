package com.soen345.project.controller;

import com.soen345.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the pending email verification code for automation only.
 * Enable with {@code e2e.expose-verification-code=true} and a non-empty {@code e2e.verification-secret};
 * the test client must send matching {@code X-E2E-Secret}. Do not enable in production.
 */
@RestController
@RequestMapping("/auth/e2e")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "e2e", name = "expose-verification-code", havingValue = "true")
public class E2eVerificationCodeController {

    private final UserRepository userRepository;

    @Value("${e2e.verification-secret:}")
    private String verificationSecret;

    @GetMapping("/verification-code")
    public ResponseEntity<?> getVerificationCode(
            @RequestParam String email,
            @RequestHeader(value = "X-E2E-Secret", required = false) String secret) {
        if (verificationSecret == null || verificationSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "E2E secret not configured"));
        }
        if (secret == null || !verificationSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        return userRepository.findByEmail(email)
                .filter(u -> u.getVerificationCode() != null && !u.getVerificationCode().isBlank())
                .filter(u -> !Boolean.TRUE.equals(u.getEnabled()))
                .map(u -> ResponseEntity.ok(Map.of("verificationCode", u.getVerificationCode())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
