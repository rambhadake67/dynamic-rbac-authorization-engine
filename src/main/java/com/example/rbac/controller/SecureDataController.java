package com.example.rbac.controller;

import com.example.rbac.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecureDataController {

    // GET /secure-data -> requires READ_SECURE_DATA permission, decided
    // dynamically by CustomPermissionEvaluator against the database.
    @GetMapping("/secure-data")
    @PreAuthorize("hasPermission(null, 'READ_SECURE_DATA')")
    public ResponseEntity<MessageResponse> getSecureData(Authentication authentication) {
        return ResponseEntity.ok(new MessageResponse(
                "Hello " + authentication.getName() + ", you have access to secure data!"));
    }
}
