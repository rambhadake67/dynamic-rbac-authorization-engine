package com.example.rbac.controller;

import com.example.rbac.dto.IdNameResponse;
import com.example.rbac.dto.MessageResponse;
import com.example.rbac.dto.NameRequest;
import com.example.rbac.entity.Role;
import com.example.rbac.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // POST /roles  -> requires CREATE_ROLE permission
    @PostMapping("/roles")
    @PreAuthorize("hasPermission(null, 'CREATE_ROLE')")
    public ResponseEntity<IdNameResponse> createRole(@Valid @RequestBody NameRequest request) {
        Role role = roleService.createRole(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IdNameResponse(role.getId(), role.getName()));
    }

    // POST /roles/{roleId}/permissions/{permissionId} -> requires ASSIGN_PERMISSION permission
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasPermission(null, 'ASSIGN_PERMISSION')")
    public ResponseEntity<MessageResponse> assignPermissionToRole(@PathVariable Long roleId,
                                                                    @PathVariable Long permissionId) {
        roleService.assignPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok(new MessageResponse(
                "Permission " + permissionId + " assigned to role " + roleId + " successfully"));
    }
}
