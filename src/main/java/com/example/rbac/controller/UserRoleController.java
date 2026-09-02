package com.example.rbac.controller;

import com.example.rbac.dto.MessageResponse;
import com.example.rbac.service.UserRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    // POST /users/{userId}/roles/{roleId} -> requires ASSIGN_ROLE permission
    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasPermission(null, 'ASSIGN_ROLE')")
    public ResponseEntity<MessageResponse> assignRoleToUser(@PathVariable Long userId,
                                                              @PathVariable Long roleId) {
        userRoleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok(new MessageResponse(
                "Role " + roleId + " assigned to user " + userId + " successfully"));
    }
}
