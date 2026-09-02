package com.example.rbac.controller;

import com.example.rbac.dto.IdNameResponse;
import com.example.rbac.dto.NameRequest;
import com.example.rbac.entity.Permission;
import com.example.rbac.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // POST /permissions -> requires CREATE_PERMISSION permission
    @PostMapping("/permissions")
    @PreAuthorize("hasPermission(null, 'CREATE_PERMISSION')")
    public ResponseEntity<IdNameResponse> createPermission(@Valid @RequestBody NameRequest request) {
        Permission permission = permissionService.createPermission(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IdNameResponse(permission.getId(), permission.getName()));
    }
}
