package com.soen345.project.controller;

import com.soen345.project.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        Map<String, Object> info = Map.of(
                "id",        currentUser.getId(),
                "firstName", currentUser.getFirstName(),
                "lastName",  currentUser.getLastName(),
                "email",     currentUser.getEmail() != null ? currentUser.getEmail() : "",
                "role",      currentUser.getRole()   // "ROLE_CUSTOMER" or "ROLE_ADMIN"
        );

        return ResponseEntity.ok(info);
    }
}
