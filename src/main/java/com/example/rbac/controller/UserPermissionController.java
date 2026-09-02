package com.example.rbac.controller;

import com.example.rbac.dto.MessageResponse;
import com.example.rbac.service.UserPermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserPermissionController {

    private final UserPermissionService userPermissionService;

    public UserPermissionController(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
    }

    // POST /users/{userId}/permissions/{permissionId} -> requires ASSIGN_PERMISSION permission
    @PostMapping("/users/{userId}/permissions/{permissionId}")
    @PreAuthorize("hasPermission(null, 'ASSIGN_PERMISSION')")
    public ResponseEntity<MessageResponse> assignPermissionToUser(@PathVariable Long userId,
                                                                   @PathVariable Long permissionId) {
        userPermissionService.assignPermissionToUser(userId, permissionId);
        return ResponseEntity.ok(new MessageResponse(
                "Permission " + permissionId + " assigned to user " + userId + " successfully"));
    }
}
